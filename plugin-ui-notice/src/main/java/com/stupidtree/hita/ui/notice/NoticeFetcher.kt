package com.stupidtree.hita.ui.notice

import java.net.HttpURLConnection
import java.net.URL

internal object NoticeFetcher {

    fun fetch(
        cookie: String,
        listUrl: (Int) -> String = NoticeParser::listUrl,
    ): List<CampusNotice> {
        require(cookie.isNotBlank()) { "请先登录门户" }
        val byId = LinkedHashMap<String, CampusNotice>()
        for (page in 1..3) {
            val url = listUrl(page)
            val html = download(url, cookie)
            val notices = NoticeParser.parse(html, url)
            if (notices.isEmpty()) break
            notices.forEach { byId.putIfAbsent(it.id, it) }
            if (byId.size >= 30) break
        }
        return byId.values.sortedByDescending { it.time }.take(30)
    }

    private fun download(url: String, cookie: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 20_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("Cookie", cookie)
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 OH-MY-HITA")
        try {
            if (NoticeParser.isLogin(conn.url.toString())) error("门户会话已失效，请重新登录")
            if (conn.responseCode in 401..403) error("门户会话已失效，请重新登录")
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
