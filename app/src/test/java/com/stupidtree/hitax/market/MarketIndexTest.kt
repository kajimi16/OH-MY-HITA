package com.stupidtree.hitax.market

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketIndexTest {

    private val gson = Gson()

    @Test
    fun parsesPluginsAndUiPlugins() {
        val index = gson.fromJson(
            """
            {
              "plugins": [{
                "id": "com.example.hello", "name": "示例", "version": "0.1.0",
                "apiVersion": 2, "url": "hello.dex", "sha256": "abc",
                "entryClass": "com.example.Hello"
              }],
              "uiPlugins": [{
                "id": "com.example.ui", "name": "界面", "version": "0.4.8",
                "url": "https://example.com/ui.apk", "sha256": "def"
              }]
            }
            """.trimIndent(),
            MarketIndex::class.java,
        )

        assertEquals(1, index.plugins.size)
        assertEquals("com.example.hello", index.plugins[0].id)
        assertEquals(1, index.uiPlugins.size)
        assertEquals("https://example.com/ui.apk", index.uiPlugins[0].url)
        assertEquals("def", index.uiPlugins[0].sha256)
    }

    @Test
    fun oldIndexWithoutUiPluginsKeepsEmptyList() {
        val index = gson.fromJson(
            """
            {"plugins":[{"id":"a","name":"A","version":"1","apiVersion":2,
            "url":"a.dex","sha256":"x","entryClass":"A"}]}
            """.trimIndent(),
            MarketIndex::class.java,
        )

        assertEquals(1, index.plugins.size)
        assertTrue(index.uiPlugins.isEmpty())
        // 旧索引没有新增元数据字段，必须有安全的默认值。
        assertNull(index.plugins[0].description)
        assertEquals(0L, index.plugins[0].size)
        assertNull(index.plugins[0].minHostApi)
        assertNull(index.plugins[0].author)
        assertNull(index.plugins[0].tags)
        assertNull(index.plugins[0].repo)
    }

    @Test
    fun parsesMarketMetadata() {
        val index = gson.fromJson(
            """
            {"uiPlugins":[{
              "id":"a","name":"A","version":"1","url":"a.apk","sha256":"x",
              "author":"someone","tags":["番剧","Bangumi"],
              "repo":"https://github.com/x/y","updatedAt":"2026-11-05"
            }]}
            """.trimIndent(),
            MarketIndex::class.java,
        )

        val entry = index.uiPlugins[0]
        assertEquals("someone", entry.author)
        assertEquals(listOf("番剧", "Bangumi"), entry.tags)
        assertEquals("https://github.com/x/y", entry.repo)
        assertEquals("2026-11-05", entry.updatedAt)
    }

    @Test
    fun emptyObjectYieldsNoPlugins() {
        val index = gson.fromJson("{}", MarketIndex::class.java)

        assertTrue(index.plugins.isEmpty())
        assertTrue(index.uiPlugins.isEmpty())
    }
}
