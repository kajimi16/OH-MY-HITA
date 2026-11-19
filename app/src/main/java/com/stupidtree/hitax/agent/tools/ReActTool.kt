package com.stupidtree.hitax.agent.tools

import android.app.Application
import com.google.gson.JsonObject
import com.stupidtree.hitax.agent.core.AgentTraceEvent
import com.stupidtree.hitax.agent.model.AgentResourceCard

data class ReActToolInput(
    /** registry 清理后的原始输入，仅供展示、插件 handler 与旧逻辑兼容。 */
    val actionInput: String,
    val userMessage: String,
    val application: Application,
    val timetableId: String?,
    val onTrace: (AgentTraceEvent) -> Unit,
    val onResourceCards: (List<AgentResourceCard>) -> Unit = {},
    /** registry 一次性解析并补全别名后的参数对象；内置工具应只读这里。 */
    val arguments: JsonObject = JsonObject(),
)

fun interface ReActTool {
    fun execute(input: ReActToolInput): String?
}

fun interface SuspendingReActTool {
    suspend fun execute(input: ReActToolInput): String?
}
