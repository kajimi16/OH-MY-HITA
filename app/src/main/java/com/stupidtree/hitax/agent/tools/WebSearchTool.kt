package com.stupidtree.hitax.agent.tools

import com.google.gson.JsonObject
import com.stupidtree.hitax.agent.remote.AgentBackendClient

class WebSearchTool : ReActTool {
    override fun execute(input: ReActToolInput): String {
        return try {
            val query = extractParam(input.arguments) ?: input.actionInput.trim()
            val result = AgentBackendClient.braveSearchSync(query)
            if (!result.ok) return "搜索失败: ${result.error?.message ?: "未知错误"}"
            if (result.results.isEmpty()) return "未找到“$query”的网页搜索结果"
            result.results.take(5).mapIndexed { index, r ->
                val title = r["title"]?.toString() ?: "搜索结果 ${index + 1}"
                val url = r["url"]?.toString().orEmpty()
                val desc = r["content"]?.toString()?.ifBlank { r["description"]?.toString() }.orEmpty()
                "$title\n$url\n$desc"
            }.joinToString("\n\n")
        } catch (e: Exception) {
            "搜索失败：${e.message ?: "未知错误"}"
        }
    }

    internal fun extractParam(arguments: JsonObject): String? =
        arguments.stringArg("param", "query", "keyword")
}
