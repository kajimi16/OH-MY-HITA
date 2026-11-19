package com.stupidtree.hitax.agent.tools

import com.google.gson.JsonObject
import com.stupidtree.hitax.agent.remote.AgentBackendClient

class RagSearchTool : ReActTool {
    override fun execute(input: ReActToolInput): String {
        return try {
            val query = extractParam(input.arguments)
                ?: input.actionInput.trim().removeSurrounding("{", "}")
            if (query.isBlank()) return "RAG 查询失败: 缺少查询词"
            AgentBackendClient.ragQuerySync(query) ?: "无法执行 RAG 查询"
        } catch (e: Exception) {
            "RAG 查询失败：${e.message ?: "未知错误"}"
        }
    }

    internal fun extractParam(arguments: JsonObject): String? =
        arguments.stringArg("query", "keyword", "param")
}
