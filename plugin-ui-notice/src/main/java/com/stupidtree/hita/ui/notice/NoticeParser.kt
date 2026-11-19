package com.stupidtree.hita.ui.notice

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal data class CampusNotice(
    val id: String,
    val title: String,
    val url: String,
    val date: String,
    val time: Long,
) {
    val pubDateMillis: Long get() = time
}

internal object NoticeParser {
    const val PORTAL = "https://info.hitsz.edu.cn/"
    const val LIST = "https://info.hitsz.edu.cn/list.jsp?urltype=tree.TreeTempUrl&wbtreeid=1053"
    const val LIST_URL = LIST
    private val dateRegex = Regex("""(20\d{2})[-/.年](\d{1,2})[-/.月](\d{1,2})""")
    private val newsId = Regex("""wbnewsid=(\d+)""", RegexOption.IGNORE_CASE)
    private val treeId = Regex("""wbtreeid=(\d+)""", RegexOption.IGNORE_CASE)

    fun listUrl(page: Int): String = if (page <= 1) LIST else "$LIST&PAGENUM=$page"
    fun listPageUrl(page: Int): String = listUrl(page)

    fun isLogin(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("ids.hit.edu.cn") || lower.contains("/authserver/") || lower.contains("caslogin")
    }

    fun isNoticeUrl(url: String): Boolean {
        val lower = url.lowercase()
        if (lower.startsWith("javascript:") || isLogin(url)) return false
        return newsId.containsMatchIn(lower) &&
            (lower.contains("newscontenturl") || lower.contains("content.jsp"))
    }

    fun parse(html: String, baseUrl: String): List<CampusNotice> {
        val root = Jsoup.parse(html, baseUrl).selectFirst("div.Newslist, .Newslist") ?: return emptyList()
        val byId = LinkedHashMap<String, CampusNotice>()
        for (anchor in root.select("a[href]")) {
            val notice = parseAnchor(anchor) ?: continue
            if (notice.id !in byId) byId[notice.id] = notice
        }
        return byId.values.sortedByDescending { it.time }.take(30)
    }

    private fun parseAnchor(anchor: Element): CampusNotice? {
        val raw = anchor.absUrl("href").ifBlank { anchor.attr("href") }.substringBefore('#').trim()
        if (raw.isBlank() || raw.startsWith("javascript:", true) || isLogin(raw)) return null
        if (!raw.contains("newscontenturl", true) && !raw.contains("content.jsp", true)) return null
        val news = newsId.find(raw)?.groupValues?.get(1) ?: return null
        val tree = treeId.find(raw)?.groupValues?.get(1) ?: "1023"
        val url = "${PORTAL}content.jsp?urltype=news.NewsContentUrl&wbtreeid=$tree&wbnewsid=$news"
        val title = anchor.attr("title").ifBlank { anchor.text() }
            .replace('\u00a0', ' ').replace(Regex("\\s+"), " ").trim()
        if (title.length < 4) return null
        val dateText = anchor.parent()?.selectFirst("span")?.text()
            ?.takeIf { it.isNotBlank() } ?: anchor.parent()?.text().orEmpty()
        val match = dateRegex.find(dateText)
        val date = if (match != null) {
            "${match.groupValues[1]}-${match.groupValues[2].padStart(2, '0')}-${match.groupValues[3].padStart(2, '0')}"
        } else ""
        val time = if (date.isBlank()) 0L else runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).apply {
                timeZone = TimeZone.getTimeZone("Asia/Shanghai")
            }.parse(date)?.time ?: 0L
        }.getOrDefault(0L)
        return CampusNotice("wbnews:$news", title, url, date, time)
    }
}
