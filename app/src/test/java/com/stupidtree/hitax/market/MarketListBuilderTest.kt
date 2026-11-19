package com.stupidtree.hitax.market

import com.stupidtree.hitax.plugin.BuiltinPluginManager
import com.stupidtree.hitax.uiplugin.InstalledUiPlugin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketListBuilderTest {

    private val uiAvailable = UiMarketPlugin(
        id = "ui.a",
        name = "课程资源",
        version = "1.1.0",
        url = "a.apk",
        sha256 = "a".repeat(64),
        description = "课程资料",
        author = "HITCS",
        tags = listOf("课程", "资源"),
    )

    private val installedUi = InstalledUiPlugin(
        id = "ui.a",
        name = "课程资源",
        version = "1.0.0",
        entryClass = "A",
        apkPath = "/tmp/a.apk",
        sha256 = "b".repeat(64),
        enabled = true,
    )

    private val dexAvailable = MarketPlugin(
        id = "dex.a",
        name = "成绩逻辑",
        version = "2.0.0",
        apiVersion = 9,
        url = "a.dex",
        sha256 = "c".repeat(64),
        entryClass = "B",
        author = "kajimi",
        tags = listOf("成绩"),
    )

    private val preset = MarketPreset(
        id = "preset.1",
        name = "HITA-L",
        version = "1.5.3",
        source = "HITA",
        description = "轻量组合",
        plugins = listOf(PresetPluginRef("dex.a", "dex")),
    )

    @Test
    fun explorePlacesUpdatesBeforePresetsAndDoesNotDuplicate() {
        val state = MarketState(
            presets = listOf(preset),
            activePresetId = null,
            availableUi = listOf(uiAvailable),
            installedUi = listOf(installedUi),
            dex = listOf(PluginMarketViewModel.Item(dexAvailable, installed = false, enabled = false)),
        )

        val rows = MarketListBuilder.build(MarketTab.EXPLORE, MarketFilters(), "", state)

        assertTrue(rows[0] is MarketItem.Section)
        assertEquals(SectionKind.UPDATES, (rows[0] as MarketItem.Section).kind)
        assertEquals("ui.a", (rows[1] as MarketItem.Ui).plugin.id)
        assertEquals(SectionKind.PRESETS, (rows[2] as MarketItem.Section).kind)
        assertEquals("preset.1", (rows[3] as MarketItem.Preset).preset.id)
        assertEquals(SectionKind.ALL_PLUGINS, (rows[4] as MarketItem.Section).kind)
        assertEquals(1, rows.count { it is MarketItem.Ui })
        assertEquals("dex.a", (rows[5] as MarketItem.Dex).item.entry.id)
    }

    @Test
    fun searchMatchesAuthorAndTags() {
        val state = MarketState(
            availableUi = listOf(uiAvailable),
            dex = listOf(PluginMarketViewModel.Item(dexAvailable, installed = false, enabled = false)),
        )

        val byAuthor = MarketListBuilder.build(MarketTab.EXPLORE, MarketFilters(), "HITCS", state)
        val byTag = MarketListBuilder.build(MarketTab.EXPLORE, MarketFilters(), "成绩", state)

        assertEquals(listOf("ui.a"), byAuthor.filterIsInstance<MarketItem.Ui>().map { it.plugin.id })
        assertEquals(listOf("dex.a"), byTag.filterIsInstance<MarketItem.Dex>().map { it.item.entry.id })
    }

    @Test
    fun installedGroupsUpdatesEnabledAndDisabled() {
        val enabled = BuiltinPluginManager.Row("builtin.a", "课表", installed = true)
        val disabled = BuiltinPluginManager.Row("builtin.b", "资讯", installed = false)
        val state = MarketState(
            availableUi = listOf(uiAvailable),
            installedUi = listOf(installedUi),
            dex = listOf(PluginMarketViewModel.Item(dexAvailable, installed = true, enabled = true, update = true)),
            builtins = listOf(enabled, disabled),
        )

        val rows = MarketListBuilder.build(MarketTab.INSTALLED, MarketFilters(), "", state)
        val sections = rows.filterIsInstance<MarketItem.Section>().map { it.kind }
        assertEquals(listOf(SectionKind.UPDATES, SectionKind.ENABLED, SectionKind.DISABLED), sections)
        assertEquals(2, rows.count { it is MarketItem.Ui || it is MarketItem.Dex })
        assertEquals(2, rows.count { it is MarketItem.Builtin })
    }

    @Test
    fun filtersAreNormalizedPerTab() {
        val explore = MarketFilters(MarketKind.APK, MarketStatus.ENABLED).normalized(MarketTab.EXPLORE)
        assertEquals(MarketKind.ALL, explore.kind)
        assertEquals(MarketStatus.ALL, explore.status)

        val installed = MarketFilters(MarketKind.PRESET, MarketStatus.AVAILABLE).normalized(MarketTab.INSTALLED)
        assertEquals(MarketKind.ALL, installed.kind)
        assertEquals(MarketStatus.ALL, installed.status)
    }
}
