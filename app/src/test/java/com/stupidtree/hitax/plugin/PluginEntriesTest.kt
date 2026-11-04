package com.stupidtree.hitax.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginEntriesTest {

    private val json = """
        {
          "enabled": ["a.plugin"],
          "plugins": [
            {"id": "a.plugin", "class": "com.example.A", "needsApplication": true},
            {"id": "b.plugin", "class": "com.example.B"}
          ]
        }
    """.trimIndent()

    @Test
    fun onlyEnabledEntriesAreLoaded() {
        val entries = PluginEntries.parse(json)
        assertEquals(listOf("a.plugin"), entries.map { it.id })
        assertTrue(entries.first().needsApplication)
    }

    @Test
    fun emptyEnabledMeansAll() {
        val all = """
            {"enabled": [], "plugins": [{"id":"x","class":"X"},{"id":"y","class":"Y"}]}
        """.trimIndent()
        assertEquals(listOf("x", "y"), PluginEntries.parse(all).map { it.id })
    }

    @Test
    fun malformedOrMissingFieldsAreSkipped() {
        assertEquals(emptyList<PluginEntry>(), PluginEntries.parse("not json"))
        assertEquals(emptyList<PluginEntry>(), PluginEntries.parse("""{"plugins":[{"id":"x"}]}"""))
    }
}
