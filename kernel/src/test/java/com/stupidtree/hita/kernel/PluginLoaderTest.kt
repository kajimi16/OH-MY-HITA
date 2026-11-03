package com.stupidtree.hita.kernel

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PluginLoaderTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun sha256MatchesKnownDigest() {
        val file = tmp.newFile("plugin.dex").apply { writeText("hello") }
        // sha256("hello")
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            PluginLoader.sha256(file),
        )
    }

    @Test
    fun sha256ChangesWhenContentChanges() {
        val a = tmp.newFile("a.dex").apply { writeText("v1") }
        val b = tmp.newFile("b.dex").apply { writeText("v2") }
        assert(PluginLoader.sha256(a) != PluginLoader.sha256(b))
    }
}
