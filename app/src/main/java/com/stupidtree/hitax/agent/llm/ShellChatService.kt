package com.stupidtree.hitax.agent.llm

import android.app.Application
import com.stupidtree.hita.api.PluginToolHandler
import com.stupidtree.hita.api.PluginToolSpec
import com.stupidtree.hita.api.normalizedType
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.agent.core.AgentTraceEvent
import com.stupidtree.hitax.agent.model.AgentResourceCard
import com.stupidtree.hita.kernel.PluginOrigin
import com.stupidtree.hitax.market.PluginTrustStore
import com.stupidtree.hitax.agent.tools.ReActToolInput
import com.stupidtree.hitax.agent.tools.ReActToolRegistry
import com.stupidtree.hitax.agent.tools.SuspendingReActTool
import com.stupidtree.hitax.agent.tools.ToolParam
import com.stupidtree.hitax.agent.tools.ToolParamType
import com.stupidtree.hitax.agent.tools.ToolSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

/** 宿主版 ReAct：只依赖已移植的工具注册表，不依赖上游 EASRepository。 */
object ShellChatService {
    private const val MAX_STEPS = 6

    private fun registry(application: Application): ReActToolRegistry {
        val registry = ReActToolRegistry.createDefault()
        // 统一脱敏：工具结果可能回显密钥/令牌；失败则 fail-closed 由 registry 处理。
        registry.guard { call ->
            if (call.name == "submit_review" &&
                listOf("评价", "点评", "review").none { it in call.userMessage.lowercase() }
            ) {
                com.stupidtree.hitax.agent.tools.ToolDecision.Deny("提交评价需要用户明确要求")
            } else {
                com.stupidtree.hitax.agent.tools.ToolDecision.Allow
            }
        }
        registry.postProcess { _, result ->
            val redacted = result.text
                .replace(Regex("""sk-[A-Za-z0-9]{16,}"""), "sk-***")
                .replace(Regex("""(?i)bearer\s+[a-z0-9._-]{16,}"""), "Bearer ***")
            if (redacted == result.text) null else result.copy(text = redacted)
        }
        val host = (application as? HApplication)?.pluginHost ?: return registry
        val trustStore = PluginTrustStore(application)
        host.pluginTools()
            .filter { binding ->
                // 来源由内核记录；内置插件随 APK 发布，第三方 dex 插件必须过 verified 白名单。
                binding.origin == PluginOrigin.BUILTIN || trustStore.isVerified(binding.pluginId)
            }
            .forEach { binding ->
                val spec = binding.spec.toToolSpec(binding.handler) ?: return@forEach
                // 插件不能悄悄覆盖宿主内置工具。
                if (registry.spec(spec.name) == null) registry.register(spec)
            }
        return registry
    }

    private fun PluginToolSpec.toToolSpec(handler: PluginToolHandler): ToolSpec? {
        if (name.isBlank() || description.isBlank()) return null
        val params = parameters.map { param ->
            val type = when (param.normalizedType()) {
                "integer" -> ToolParamType.INTEGER
                "number" -> ToolParamType.NUMBER
                "boolean" -> ToolParamType.BOOLEAN
                else -> ToolParamType.STRING
            }
            ToolParam(
                name = param.name,
                type = type,
                required = param.required,
                description = param.description,
                aliases = param.aliases,
                enumValues = param.enumValues,
            )
        }
        return ToolSpec(
            name = name,
            description = description,
            parameters = params,
            outputSchemaJson = outputSchemaJson.take(2_000),
            maxObservationChars = maxObservationChars.coerceIn(1, 20_000),
            timeoutMs = timeoutMs.coerceIn(1, 120_000),
            retrySafe = retrySafe,
            allowRawString = allowRawString,
            tool = SuspendingReActTool { input ->
                runInterruptible(Dispatchers.IO) { handler.execute(input.actionInput) }
            },
        )
    }

    suspend fun chat(
        history: List<ChatMessage>,
        application: Application,
        timetableId: String?,
        onTrace: (AgentTraceEvent) -> Unit = {},
    ): LlmChatResult = withContext(Dispatchers.IO) {
        val userMessage = history.lastOrNull { it.role == "user" }?.content.orEmpty()
        if (userMessage.isBlank()) return@withContext LlmChatResult.Error("消息为空")
        val toolRegistry = registry(application)
        val messages = mutableListOf(ChatMessage(role = "system", content = systemPrompt(toolRegistry)))
        messages += history
        val observations = mutableListOf<String>()
        val cards = mutableListOf<AgentResourceCard>()

        repeat(MAX_STEPS) { step ->
            onTrace(AgentTraceEvent(stage = "react_step", message = "步骤 ${step + 1}/$MAX_STEPS", payload = ""))
            val response = runCatching {
                LlmClient.chatCompletion(
                    application,
                    ChatCompletionRequest(model = LlmClient.MODEL, messages = messages),
                )
            }.getOrElse { return@withContext LlmChatResult.Error(it.message ?: "网络错误") }
            if (!response.isSuccessful) {
                return@withContext LlmChatResult.Error("LLM HTTP ${response.code()}")
            }
            val content = response.body()?.choices?.firstOrNull()?.message?.content
                ?: return@withContext LlmChatResult.Error("LLM 返回为空")
            val parsed = parseStep(content)
            if (parsed.action == "答案" || parsed.action.isBlank()) {
                val answer = parsed.thought.ifBlank { content }.trim()
                return@withContext LlmChatResult.Success(
                    answer,
                    observations.joinToString("\n"),
                    cards.distinctBy { "${it.source}|${it.path}|${it.courseCode}|${it.title}" }.take(20),
                )
            }
            val toolInput = ReActToolInput(
                actionInput = parsed.actionInput,
                userMessage = userMessage,
                application = application,
                timetableId = timetableId,
                onTrace = onTrace,
            )
            val execution = toolRegistry.execute(parsed.action, toolInput, onTrace)
            if (!execution.error) cards += execution.cards
            val observation = execution.text
            observations += "【${parsed.action}】\n$observation"
            val presentation = toolRegistry.spec(parsed.action)
                ?.presentResult
                ?.invoke(execution)
                ?: observation.take(80)
            val meta = buildList {
                if (execution.error) add("error")
                if (execution.truncated) add("truncated")
                if (execution.retryable) add("retryable")
            }.joinToString(",")
            onTrace(
                AgentTraceEvent(
                    stage = "observation",
                    message = "观察：${presentation.take(80)}",
                    payload = buildString {
                        if (meta.isNotEmpty()) append("[$meta] ")
                        append(observation.take(500))
                    },
                )
            )
            messages += ChatMessage(role = "assistant", content = content)
            messages += ChatMessage(role = "user", content = "观察结果：$observation")
        }
        LlmChatResult.Success(
            buildString {
                append("达到最大步骤限制。已获得的信息：\n\n")
                append(observations.takeLast(4).joinToString("\n\n"))
            }.trim(),
            observations.joinToString("\n"),
            cards.distinctBy { "${it.source}|${it.path}|${it.courseCode}|${it.title}" }.take(20),
        )
    }

    private fun systemPrompt(toolRegistry: ReActToolRegistry): String = buildString {
        append("你是 HITA 课表助手。按 ReAct 格式回答。\n")
        append("格式：\n思考：...\n动作：工具名\n动作输入：<JSON>\n")
        append("或者：\n答案：...\n\n")
        append("工具目录：\n")
        append(toolRegistry.catalog())
        append("\n\n动作输入必须是合法 JSON，不要输出 Markdown 代码块。")
        append("不要编造课表、空教室或教师数据；不知道就说明。")
    }

    internal data class ParsedStep(val thought: String, val action: String, val actionInput: String)

    internal fun parseStep(text: String): ParsedStep {
        val cleaned = text.replace(Regex("""<think>.*?</think>""", RegexOption.DOT_MATCHES_ALL), "").trim()
        val thought = Regex("(?is)思考[：:]\\s*(.+?)(?=\\n动作[：:]|\\n答案[：:]|$)")
            .find(cleaned)?.groupValues?.get(1)?.trim().orEmpty()
        val action = Regex("(?im)^[ \\t]*动作[：:]\\s*(\\S+)")
            .find(cleaned)?.groupValues?.get(1)?.trim().orEmpty()
        val actionInput = Regex("(?is)动作输入[：:]\\s*(.+?)(?=\\n思考[：:]|\\n观察[：:]|$)")
            .find(cleaned)?.groupValues?.get(1)?.trim().orEmpty()
        val answer = Regex("(?is)答案[：:]\\s*(.+)")
            .find(cleaned)?.groupValues?.get(1)?.trim()
        if (action.isBlank() && answer != null) return ParsedStep(answer, "答案", "")
        if (thought.isBlank() && action.isBlank()) return ParsedStep(cleaned, "答案", "")
        return ParsedStep(thought, action, actionInput)
    }
}

sealed class LlmChatResult {
    data class Success(
        val text: String,
        val thinking: String? = null,
        val cards: List<AgentResourceCard> = emptyList(),
    ) : LlmChatResult()
    data class Error(val error: String) : LlmChatResult()
}
