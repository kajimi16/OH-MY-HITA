package com.stupidtree.hitax.ui.main

import org.junit.Assert.assertEquals
import org.junit.Test

class PluginUnavailableTitleTest {
    @Test
    fun catalogNameWinsForInstalledPlugin() {
        assertEquals("时间表", resolvePluginUnavailableTitle("时间表", "今日", "hita.timeline"))
    }

    @Test
    fun presetTitleUsedWhenCatalogHasNoName() {
        assertEquals("资讯", resolvePluginUnavailableTitle(null, "资讯", "com.example.news"))
    }

    @Test
    fun pluginIdIsLastResort() {
        assertEquals("com.example.news", resolvePluginUnavailableTitle(null, " ", "com.example.news"))
    }
}
