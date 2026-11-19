package com.stupidtree.hitax.agent.tools

import com.google.gson.JsonObject
import com.stupidtree.hitax.agent.remote.AgentBackendClient

class CrawlPageTool : ReActTool {
    override fun execute(input: ReActToolInput): String {
        return try {
            val url = input.arguments.stringArg("url", "param")
                ?: input.actionInput.trim().removeSurrounding("{", "}").trim('"')
            val page = AgentBackendClient.crawlPageSync(url) ?: return "无法获取页面内容"
            val range = extractCharRange(input.arguments) ?: TextRange(0, DEFAULT_PAGE_CHARS.coerceAtMost(page.length))
            val start = range.start.coerceIn(0, page.length)
            val end = range.end.coerceIn(start, page.length)
            buildString {
                append("网页内容（")
                append(url)
                append("）\n")
                append(page.substring(start, end))
                append("\n\n（当前显示字符 ${start}-${end} / ${page.length}。）")
                if (end < page.length) {
                    append(" 如需继续读取，可再次调用 crawl_page 并传入 {\"url\":\"")
                    append(url)
                    append("\",\"start\":")
                    append(end)
                    append(",\"end\":")
                    append((end + DEFAULT_PAGE_CHARS).coerceAtMost(page.length))
                    append("}。")
                }
            }
        } catch (e: Exception) {
            "读取网页失败：${e.message ?: "未知错误"}"
        }
    }

    internal fun extractCharRange(arguments: JsonObject): TextRange? {
        val start = arguments.intArg("start", "char_start", "offset", "from")
        val end = arguments.intArg("end", "char_end", "to")
        val limit = arguments.intArg("limit", "length")
        return when {
            start != null && end != null -> TextRange(start, end)
            start != null && limit != null -> TextRange(start, start + limit)
            start != null -> TextRange(start, start + DEFAULT_PAGE_CHARS)
            else -> null
        }
    }

    internal data class TextRange(val start: Int, val end: Int)

    private companion object {
        const val DEFAULT_PAGE_CHARS = 5000
    }
}
