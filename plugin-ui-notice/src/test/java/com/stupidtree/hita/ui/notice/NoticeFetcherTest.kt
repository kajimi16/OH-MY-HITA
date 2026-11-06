package com.stupidtree.hita.ui.notice

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NoticeFetcherTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun fetchInjectsCookieAndParses() {
        val html = """
            <div class="Newslist"><ul>
            <li><span>2026-09-22</span><a href="content.jsp?urltype=news.NewsContentUrl&wbtreeid=1023&wbnewsid=1" title="测试通知一">x</a></li>
            </ul></div>
        """.trimIndent()
        repeat(3) { server.enqueue(MockResponse().setBody(html)) }
        val base = server.url("/list").toString()
        val notices = NoticeFetcher.fetch("JSESSIONID=abc") { base }
        assertEquals("JSESSIONID=abc", server.takeRequest().getHeader("Cookie"))
        assertEquals(1, notices.size)
        assertEquals("测试通知一", notices[0].title)
        assertTrue(notices[0].url.contains("wbnewsid=1"))
    }

    @Test
    fun rejects401AsLoginExpired() {
        server.enqueue(MockResponse().setResponseCode(401))
        val base = server.url("/list").toString()
        val error = runCatching { NoticeFetcher.fetch("JSESSIONID=abc") { base } }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("会话已失效"))
    }
}
