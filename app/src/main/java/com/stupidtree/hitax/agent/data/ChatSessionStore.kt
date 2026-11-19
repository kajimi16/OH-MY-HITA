package com.stupidtree.hitax.agent.data

import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stupidtree.hitax.agent.model.AgentResourceCard
import com.stupidtree.hitax.agent.model.ChatMessageEntity
import com.stupidtree.hitax.agent.model.ChatMessageRecord
import com.stupidtree.hitax.agent.model.ChatSession

class ChatSessionStore(private val database: ChatDatabase) {
    private val sessionDao = database.sessionDao()
    private val messageDao = database.messageDao()
    private val gson = Gson()
    private val cardsType = object : TypeToken<List<AgentResourceCard>>() {}.type

    val sessions: LiveData<List<ChatSession>> = sessionDao.getAll()

    fun create(session: ChatSession) = database.runInTransaction {
        if (!sessionDao.exists(session.id)) sessionDao.save(session)
    }

    fun latest(): ChatSession? = sessionDao.getLatest()

    fun restore(sessionId: String): List<ChatMessageRecord> =
        messageDao.getBySessionSync(sessionId).map { entity ->
            ChatMessageRecord(
                role = ChatMessageRecord.Role.valueOf(entity.role),
                text = entity.text,
                resourceCards = if (entity.resourceCardsJson.isBlank()) emptyList() else try {
                    gson.fromJson<List<AgentResourceCard>>(entity.resourceCardsJson, cardsType).orEmpty()
                } catch (_: Exception) {
                    emptyList()
                },
                timestampMs = entity.timestampMs,
            )
        }

    fun append(sessionId: String, message: ChatMessageRecord): Boolean {
        if (!sessionDao.exists(sessionId)) return false
        messageDao.save(
            ChatMessageEntity(
                sessionId = sessionId,
                role = message.role.name,
                text = message.text,
                resourceCardsJson = if (message.resourceCards.isEmpty()) "" else gson.toJson(message.resourceCards),
                timestampMs = message.timestampMs,
            )
        )
        val text = messageDao.getFirstUserTextSync(sessionId)
        val title = text?.take(20)?.plus(if ((text?.length ?: 0) > 20) "…" else "") ?: "新对话"
        sessionDao.updateTitle(sessionId, title)
        return true
    }

    fun delete(session: ChatSession): ChatSession? {
        sessionDao.delete(session)
        return sessionDao.getLatest()
    }
}
