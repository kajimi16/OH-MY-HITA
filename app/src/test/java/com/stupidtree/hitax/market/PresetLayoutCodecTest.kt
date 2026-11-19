package com.stupidtree.hitax.market

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PresetLayoutCodecTest {
    @Test
    fun roundTripKeepsOrderTitlesAndIcons() {
        val tabs = listOf(
            PresetTab("hita.timeline", "今日", order = 0, iconAsset = "builtin:today"),
            PresetTab("com.stupidtree.hita.ui.homework", "待办", order = 1),
        )

        val encoded = PresetLayoutCodec.encode(tabs)
        val decoded = PresetLayoutCodec.decode(encoded)

        assertEquals(tabs, decoded)
    }

    @Test
    fun emptyAndBlankTabsAreNotPersisted() {
        assertNull(PresetLayoutCodec.encode(null))
        assertNull(PresetLayoutCodec.encode(emptyList()))
        assertNull(PresetLayoutCodec.encode(listOf(PresetTab("", "坏数据"))))
    }

    @Test
    fun corruptJsonFallsBackToEmpty() {
        assertEquals(emptyList<PresetTab>(), PresetLayoutCodec.decode("not-json"))
        assertEquals(emptyList<PresetTab>(), PresetLayoutCodec.decode(""))
        assertEquals(emptyList<PresetTab>(), PresetLayoutCodec.decode(null))
    }

    @Test
    fun blankIdsAreFiltered() {
        val json = """[{"id":"","title":"blank"},{"id":"hita.more","title":"更多"}]"""
        assertEquals(listOf("hita.more"), PresetLayoutCodec.decode(json).map { it.id })
    }
}
