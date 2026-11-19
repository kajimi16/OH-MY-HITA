package com.stupidtree.hitax.market

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class IndexCacheTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun saveAndLoadKeepsBaseAndRaw() {
        val dir = tmp.newFolder("cache")
        IndexCache.save(dir, "https://example.com/market/index.json", """{"plugins":[]}""", 123L)

        val cached = IndexCache.load(dir)

        assertEquals("https://example.com/market/index.json", cached?.base)
        assertEquals("""{"plugins":[]}""", cached?.raw)
        assertEquals(123L, cached?.savedAt)
    }

    @Test
    fun missingCacheReturnsNull() {
        assertNull(IndexCache.load(tmp.newFolder("empty")))
    }

    @Test
    fun malformedCacheReturnsNull() {
        val dir = tmp.newFolder("broken")
        java.io.File(dir, "index.json").writeText("not json")

        assertNull(IndexCache.load(dir))
    }
}
