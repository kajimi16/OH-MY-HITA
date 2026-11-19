package com.stupidtree.hitax.agent.tools

import com.google.gson.JsonObject
import com.stupidtree.hitax.agent.remote.AgentBackendClient

class CrawlSiteTool : ReActTool {
    override fun execute(input: ReActToolInput): String {
        return try {
            val url = input.arguments.stringArg("url") ?: input.actionInput.trim()
            val maxPages = extractMaxPages(input.arguments)
            AgentBackendClient.crawlSiteSync(url, maxPages) ?: "无法启动站点爬取"
        } catch (e: Exception) {
            "启动站点爬取失败：${e.message ?: "未知错误"}"
        }
    }

    internal fun extractMaxPages(arguments: JsonObject): Int =
        arguments.intArg("max_pages") ?: 10
}
