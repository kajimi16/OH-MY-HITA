package com.stupidtree.hitax.agent

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.stupidtree.hitax.agent.attachment.ChatAttachmentResult
import com.stupidtree.hitax.agent.data.ChatSessionRepository
import com.stupidtree.hitax.agent.llm.ChatMessage
import com.stupidtree.hitax.agent.llm.LlmChatResult
import com.stupidtree.hitax.agent.llm.ShellChatService
import com.stupidtree.hitax.agent.llm.ZhipuChatRequest
import com.stupidtree.hitax.agent.llm.ZhipuClient
import com.stupidtree.hitax.agent.llm.ZhipuContent
import com.stupidtree.hitax.agent.llm.ZhipuImageUrl
import com.stupidtree.hitax.agent.llm.ZhipuMessage
import com.stupidtree.hitax.agent.llm.ZhipuVideoUrl
import com.stupidtree.hitax.agent.llm.awaitCancellable
import com.stupidtree.hitax.agent.model.AgentResourceCard
import com.stupidtree.hitax.agent.model.ChatMessageRecord
import com.stupidtree.hitax.agent.model.ChatSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class AgentMessage(
    val role: String,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val cards: List<AgentResourceCard> = emptyList(),
)

class AgentChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChatSessionRepository.get(application)
    val messages = mutableStateListOf<AgentMessage>()
    val sessions: LiveData<List<ChatSession>> get() = repository.sessions
    var currentSessionId by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    private var initJob: Job? = null
    private val initMutex = Mutex()

    init {
        initJob = viewModelScope.launch { ensureSession() }
    }

    private suspend fun ensureSession(): String = initMutex.withLock {
        currentSessionId?.let { return@withLock it }
        val latest = repository.latest().await()
        val session = latest ?: ChatSession().also { repository.create(it).await() }
        currentSessionId = session.id
        restore(session.id)
        session.id
    }

    private suspend fun restore(sessionId: String) {
        val records = repository.restore(sessionId).await()
        messages.clear()
        records.filter { it.role != ChatMessageRecord.Role.TRACE }.forEach {
            messages += AgentMessage(
                it.role.name.lowercase(),
                it.text,
                it.timestampMs,
                it.resourceCards,
            )
        }
    }

    fun send(text: String, timetableId: String?) {
        val content = text.trim()
        if (content.isEmpty() || busy) return
        busy = true
        viewModelScope.launch {
            try {
                initJob?.join()
                val sessionId = ensureSession()
                append(sessionId, ChatMessageRecord.Role.USER, content)
                messages += AgentMessage("user", content)
                val history = messages
                    .filter { it.role == "user" || it.role == "assistant" }
                    .map { ChatMessage(role = it.role, content = it.text) }
                when (val result = ShellChatService.chat(history, getApplication(), timetableId)) {
                    is LlmChatResult.Success -> {
                        append(sessionId, ChatMessageRecord.Role.ASSISTANT, result.text, result.cards)
                        messages += AgentMessage("assistant", result.text, cards = result.cards)
                    }
                    is LlmChatResult.Error -> {
                        val error = "出错了：${result.error}"
                        append(sessionId, ChatMessageRecord.Role.ASSISTANT, error)
                        messages += AgentMessage("assistant", error)
                    }
                }
            } finally {
                busy = false
            }
        }
    }

    private suspend fun append(
        sessionId: String,
        role: ChatMessageRecord.Role,
        text: String,
        cards: List<AgentResourceCard> = emptyList(),
    ) {
        repository.append(
            sessionId,
            ChatMessageRecord(role = role, text = text, resourceCards = cards),
        ).await()
    }

    fun sendMedia(prompt: String, media: ChatAttachmentResult.Media, timetableId: String?) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                initJob?.join()
                val sessionId = ensureSession()
                val text = prompt.trim().ifEmpty { "请分析这个附件" }
                val display = "$text\n[附件: ${media.fileName}]"
                append(sessionId, ChatMessageRecord.Role.USER, display)
                messages += AgentMessage("user", display)
                val history = messages
                    .filter { it.role == "user" || it.role == "assistant" }
                    .dropLast(1)
                    .map { ZhipuMessage(it.role, listOf(ZhipuContent(type = "text", text = it.text))) }
                val mediaContent = if (media.mimeType.startsWith("video/")) {
                    ZhipuContent(type = "video_url", video_url = ZhipuVideoUrl("data:${media.mimeType};base64,${media.base64}"))
                } else {
                    ZhipuContent(type = "image_url", image_url = ZhipuImageUrl("data:${media.mimeType};base64,${media.base64}"))
                }
                val response = runCatching {
                    ZhipuClient.service.chatCompletion(
                        ZhipuClient.authHeader(getApplication()),
                        ZhipuChatRequest(messages = history + ZhipuMessage("user", listOf(
                            ZhipuContent(type = "text", text = text),
                            mediaContent,
                        ))),
                    ).awaitCancellable()
                }.getOrElse { error ->
                    val message = "出错了：${error.message ?: "多模态请求失败"}"
                    append(sessionId, ChatMessageRecord.Role.ASSISTANT, message)
                    messages += AgentMessage("assistant", message)
                    return@launch
                }
                val answer = response.body()?.choices?.firstOrNull()?.message?.content
                if (!response.isSuccessful || answer.isNullOrBlank()) {
                    val detail = when (response.code()) {
                        429 -> "多模态模型限流，请稍后再试"
                        401, 403 -> "智谱 API Key 无效或无多模态权限"
                        400 -> "多模态请求被拒绝"
                        else -> "多模态模型 HTTP ${response.code()}"
                    }
                    val message = "出错了：$detail"
                    append(sessionId, ChatMessageRecord.Role.ASSISTANT, message)
                    messages += AgentMessage("assistant", message)
                } else {
                    append(sessionId, ChatMessageRecord.Role.ASSISTANT, answer)
                    messages += AgentMessage("assistant", answer)
                }
            } finally {
                busy = false
            }
        }
    }

    /** 会话列表切换；发送中不切换，避免回复写错会话。 */
    fun switchToSession(sessionId: String) {
        if (busy || sessionId == currentSessionId) return
        viewModelScope.launch {
            initJob?.join()
            currentSessionId = sessionId
            restore(sessionId)
        }
    }

    fun createNewSession() {
        if (busy) return
        viewModelScope.launch {
            initJob?.join()
            val session = ChatSession()
            repository.create(session).await()
            currentSessionId = session.id
            messages.clear()
        }
    }

    fun deleteSession(session: ChatSession) {
        if (busy) return
        viewModelScope.launch {
            initJob?.join()
            val latest = repository.delete(session).await()
            if (currentSessionId == session.id) {
                if (latest == null) createNewSession() else {
                    currentSessionId = latest.id
                    restore(latest.id)
                }
            }
        }
    }
}
