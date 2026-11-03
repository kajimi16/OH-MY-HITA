package com.stupidtree.hitax.market

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginUrlsTest {

    private val remoteIndex = "https://raw.githubusercontent.com/kajimi16/OH-MY-HITA/master/market/index.json"

    @Test
    fun absoluteUrlIsKeptAsIs() {
        assertEquals(
            "https://cdn.example.com/p.dex",
            PluginUrls.resolve("https://cdn.example.com/p.dex", remoteIndex),
        )
    }

    @Test
    fun relativeUrlResolvesAgainstRemoteIndex() {
        assertEquals(
            "https://raw.githubusercontent.com/kajimi16/OH-MY-HITA/master/market/hello.dex",
            PluginUrls.resolve("hello.dex", remoteIndex),
        )
        assertEquals(
            "https://raw.githubusercontent.com/kajimi16/OH-MY-HITA/master/market/sub/p.dex",
            PluginUrls.resolve("sub/p.dex", remoteIndex),
        )
    }

    @Test
    fun relativeUrlResolvesAgainstLocalDirectory() {
        val resolved = PluginUrls.resolve("hello.dex", "/data/user/0/app/files/market")
        assertEquals("/data/user/0/app/files/market/hello.dex", resolved)
        assertTrue(resolved.startsWith("/"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingBaseForRelativeUrlFails() {
        PluginUrls.resolve("hello.dex", null)
    }
}
