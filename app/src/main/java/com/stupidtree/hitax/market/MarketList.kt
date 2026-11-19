package com.stupidtree.hitax.market

import com.stupidtree.hitax.plugin.BuiltinPluginManager
import com.stupidtree.hitax.uiplugin.InstalledUiPlugin

/**
 * 插件市场的列表状态与纯函数构建器。
 *
 * 这里不依赖 Context/View，便于单测；Adapter 只负责把 [MarketItem] 绑定到视图。
 */

enum class MarketTab { EXPLORE, INSTALLED, UPDATES }

enum class MarketKind { ALL, PRESET, UI, DEX, BUILTIN, APK }

enum class MarketStatus { ALL, AVAILABLE, INSTALLED, UPDATES, ENABLED, DISABLED }

enum class SectionKind { UPDATES, PRESETS, ALL_PLUGINS, ENABLED, DISABLED }

enum class SectionAction { UPDATE_ALL, CLEAR_FILTERS }

data class MarketFilters(
    val kind: MarketKind = MarketKind.ALL,
    val status: MarketStatus = MarketStatus.ALL,
) {
    fun activeCount(): Int =
        (if (kind != MarketKind.ALL) 1 else 0) + (if (status != MarketStatus.ALL) 1 else 0)

    fun normalized(tab: MarketTab): MarketFilters {
        val kinds = when (tab) {
            MarketTab.EXPLORE -> setOf(MarketKind.ALL, MarketKind.PRESET, MarketKind.UI, MarketKind.DEX)
            MarketTab.INSTALLED -> setOf(
                MarketKind.ALL,
                MarketKind.UI,
                MarketKind.DEX,
                MarketKind.BUILTIN,
                MarketKind.APK,
            )
            MarketTab.UPDATES -> setOf(MarketKind.ALL, MarketKind.UI, MarketKind.DEX)
        }
        val statuses = when (tab) {
            MarketTab.EXPLORE -> setOf(
                MarketStatus.ALL,
                MarketStatus.AVAILABLE,
                MarketStatus.INSTALLED,
                MarketStatus.UPDATES,
            )
            MarketTab.INSTALLED -> setOf(
                MarketStatus.ALL,
                MarketStatus.ENABLED,
                MarketStatus.DISABLED,
                MarketStatus.UPDATES,
            )
            MarketTab.UPDATES -> setOf(MarketStatus.ALL)
        }
        return MarketFilters(
            kind = if (kind in kinds) kind else MarketKind.ALL,
            status = if (status in statuses) status else MarketStatus.ALL,
        )
    }
}

/** 一次渲染所需的全部输入；Fragment 从 LiveData 汇总。 */
data class MarketState(
    val presets: List<MarketPreset> = emptyList(),
    val activePresetId: String? = null,
    val availableUi: List<UiMarketPlugin> = emptyList(),
    val installedUi: List<InstalledUiPlugin> = emptyList(),
    val dex: List<PluginMarketViewModel.Item> = emptyList(),
    val builtins: List<BuiltinPluginManager.Row> = emptyList(),
    val apk: List<ApkPlugin> = emptyList(),
    val apkDisabled: Set<String> = emptySet(),
    val pendingUpdates: Int = 0,
)

sealed class MarketItem {
    abstract val id: String

    data class Section(
        override val id: String,
        val kind: SectionKind,
        val count: Int,
        val action: SectionAction? = null,
        val subtitle: String? = null,
    ) : MarketItem()

    data class Preset(
        val preset: MarketPreset,
        val active: Boolean,
    ) : MarketItem() {
        override val id: String = "preset:${preset.id}"
    }

    data class Ui(
        val plugin: UiMarketPlugin,
        val installed: InstalledUiPlugin?,
        val update: Boolean,
        val tab: MarketTab,
    ) : MarketItem() {
        override val id: String = "ui:${plugin.id}"
    }

    data class Dex(
        val item: PluginMarketViewModel.Item,
        val tab: MarketTab,
    ) : MarketItem() {
        override val id: String = "dex:${item.entry.id}"
    }

    data class Builtin(
        val row: BuiltinPluginManager.Row,
        val tab: MarketTab,
    ) : MarketItem() {
        override val id: String = "builtin:${row.id}"
    }

    data class Apk(
        val plugin: ApkPlugin,
        val enabled: Boolean,
        val tab: MarketTab,
    ) : MarketItem() {
        override val id: String = "apk:${plugin.packageName}"
    }
}

object MarketListBuilder {

    fun build(
        tab: MarketTab,
        filters: MarketFilters,
        query: String,
        state: MarketState,
    ): List<MarketItem> {
        val f = filters.normalized(tab)
        val q = query.trim()
        return when (tab) {
            MarketTab.EXPLORE -> explore(f, q, state)
            MarketTab.INSTALLED -> installed(f, q, state)
            MarketTab.UPDATES -> updates(f, q, state)
        }
    }

    private fun explore(f: MarketFilters, q: String, s: MarketState): List<MarketItem> {
        val installedUi = s.installedUi.associateBy { it.id }
        val updates = buildList {
            if (f.kind in setOf(MarketKind.ALL, MarketKind.UI)) {
                s.availableUi.forEach { plugin ->
                    val installed = installedUi[plugin.id] ?: return@forEach
                    if (PluginVersion.isNewer(plugin.version, installed.version) &&
                        matches(q, plugin, "界面插件 ui")
                    ) {
                        add(MarketItem.Ui(plugin, installed, true, MarketTab.EXPLORE))
                    }
                }
            }
            if (f.kind in setOf(MarketKind.ALL, MarketKind.DEX)) {
                s.dex.filter { it.update && matches(q, it, "逻辑插件 dex") }
                    .forEach { add(MarketItem.Dex(it, MarketTab.EXPLORE)) }
            }
        }.sortedBy { it.nameForSort() }

        val showUpdates = updates.isNotEmpty() && f.status in setOf(MarketStatus.ALL, MarketStatus.UPDATES)
        val presetRows = if (f.kind in setOf(MarketKind.ALL, MarketKind.PRESET)) {
            s.presets.filter { preset ->
                matchesPreset(q, preset) && presetMatchesStatus(preset, s.activePresetId, f.status)
            }.map { MarketItem.Preset(it, it.id == s.activePresetId) }
        } else {
            emptyList()
        }
        val pluginRows = if (f.status == MarketStatus.UPDATES) {
            emptyList()
        } else {
            buildList {
                if (f.kind in setOf(MarketKind.ALL, MarketKind.UI)) {
                    s.availableUi.forEach { plugin ->
                        val installed = installedUi[plugin.id]
                        val update = installed != null && PluginVersion.isNewer(plugin.version, installed.version)
                        if (showUpdates && update) return@forEach
                        if (matches(q, plugin, "界面插件 ui") && statusMatches(installed != null, update, f.status)) {
                            add(MarketItem.Ui(plugin, installed, update, MarketTab.EXPLORE))
                        }
                    }
                }
                if (f.kind in setOf(MarketKind.ALL, MarketKind.DEX)) {
                    s.dex.forEach { item ->
                        if (showUpdates && item.update) return@forEach
                        if (matches(q, item, "逻辑插件 dex") && statusMatches(item.installed, item.update, f.status)) {
                            add(MarketItem.Dex(item, MarketTab.EXPLORE))
                        }
                    }
                }
            }.sortedBy { it.nameForSort() }
        }

        return buildList {
            if (showUpdates) {
                add(MarketItem.Section("section:updates", SectionKind.UPDATES, updates.size, SectionAction.UPDATE_ALL))
                addAll(updates)
            }
            if (presetRows.isNotEmpty()) {
                add(MarketItem.Section("section:presets", SectionKind.PRESETS, presetRows.size))
                addAll(presetRows)
            }
            if (pluginRows.isNotEmpty()) {
                add(MarketItem.Section("section:all", SectionKind.ALL_PLUGINS, pluginRows.size))
                addAll(pluginRows)
            }
        }
    }

    private fun installed(f: MarketFilters, q: String, s: MarketState): List<MarketItem> {
        val availableUi = s.availableUi.associateBy { it.id }
        val entries = buildList {
            if (f.kind in setOf(MarketKind.ALL, MarketKind.UI)) {
                s.installedUi.filter { matches(q, it.name, it.id, "界面插件 ui") }.forEach { installed ->
                    val available = availableUi[installed.id]
                    val update = available != null && PluginVersion.isNewer(available.version, installed.version)
                    add(MarketItem.Ui(
                        plugin = available ?: UiMarketPlugin(
                            id = installed.id,
                            name = installed.name,
                            version = installed.version,
                            url = "",
                            sha256 = installed.sha256,
                        ),
                        installed = installed,
                        update = update,
                        tab = MarketTab.INSTALLED,
                    ))
                }
            }
            if (f.kind in setOf(MarketKind.ALL, MarketKind.DEX)) {
                s.dex.filter { it.installed && matches(q, it, "逻辑插件 dex") }
                    .forEach { add(MarketItem.Dex(it, MarketTab.INSTALLED)) }
            }
            if (f.kind in setOf(MarketKind.ALL, MarketKind.BUILTIN)) {
                s.builtins.filter { matches(q, it.name, it.id, "内置插件 builtin") }
                    .forEach { add(MarketItem.Builtin(it, MarketTab.INSTALLED)) }
            }
            if (f.kind in setOf(MarketKind.ALL, MarketKind.APK)) {
                s.apk.filter { matches(q, it.name, it.packageName, "独立 apk") }
                    .forEach { add(MarketItem.Apk(it, it.packageName !in s.apkDisabled, MarketTab.INSTALLED)) }
            }
        }

        fun updateOf(item: MarketItem): Boolean = when (item) {
            is MarketItem.Ui -> item.update
            is MarketItem.Dex -> item.item.update
            else -> false
        }

        fun enabledOf(item: MarketItem): Boolean = when (item) {
            is MarketItem.Ui -> item.installed?.enabled == true
            is MarketItem.Dex -> item.item.enabled
            is MarketItem.Builtin -> item.row.installed
            is MarketItem.Apk -> item.enabled
            else -> false
        }

        val updates = entries.filter { updateOf(it) }.sortedBy { it.nameForSort() }
        val showUpdateSection = updates.isNotEmpty() && f.status in setOf(MarketStatus.ALL, MarketStatus.UPDATES)
        val enabled = entries.filter {
            enabledOf(it) && !(showUpdateSection && updateOf(it)) &&
                f.status in setOf(MarketStatus.ALL, MarketStatus.ENABLED)
        }.sortedBy { it.nameForSort() }
        val disabled = entries.filter {
            !enabledOf(it) && !(showUpdateSection && updateOf(it)) &&
                f.status in setOf(MarketStatus.ALL, MarketStatus.DISABLED)
        }.sortedBy { it.nameForSort() }

        return buildList {
            if (f.status == MarketStatus.UPDATES) {
                if (updates.isEmpty()) return@buildList
                add(MarketItem.Section("section:updates", SectionKind.UPDATES, updates.size, SectionAction.UPDATE_ALL))
                addAll(updates)
                return@buildList
            }
            if (showUpdateSection) {
                add(MarketItem.Section("section:updates", SectionKind.UPDATES, updates.size, SectionAction.UPDATE_ALL))
                addAll(updates)
            }
            if (enabled.isNotEmpty()) {
                add(MarketItem.Section("section:enabled", SectionKind.ENABLED, enabled.size))
                addAll(enabled)
            }
            if (disabled.isNotEmpty()) {
                add(MarketItem.Section("section:disabled", SectionKind.DISABLED, disabled.size))
                addAll(disabled)
            }
        }
    }

    private fun updates(f: MarketFilters, q: String, s: MarketState): List<MarketItem> {
        val installedUi = s.installedUi.associateBy { it.id }
        val rows = buildList {
            if (f.kind in setOf(MarketKind.ALL, MarketKind.UI)) {
                s.availableUi.forEach { plugin ->
                    val installed = installedUi[plugin.id] ?: return@forEach
                    if (PluginVersion.isNewer(plugin.version, installed.version) &&
                        matches(q, plugin, "界面插件 ui")
                    ) {
                        add(MarketItem.Ui(plugin, installed, true, MarketTab.UPDATES))
                    }
                }
            }
            if (f.kind in setOf(MarketKind.ALL, MarketKind.DEX)) {
                s.dex.filter { it.update && matches(q, it, "逻辑插件 dex") }
                    .forEach { add(MarketItem.Dex(it, MarketTab.UPDATES)) }
            }
        }.sortedBy { it.nameForSort() }
        if (rows.isEmpty()) return emptyList()
        return listOf(
            MarketItem.Section("section:updates", SectionKind.UPDATES, rows.size, SectionAction.UPDATE_ALL),
        ) + rows
    }

    private fun statusMatches(installed: Boolean, update: Boolean, status: MarketStatus): Boolean =
        when (status) {
            MarketStatus.ALL -> true
            MarketStatus.AVAILABLE -> !installed
            MarketStatus.INSTALLED -> installed
            MarketStatus.UPDATES -> update
            MarketStatus.ENABLED, MarketStatus.DISABLED -> true
        }

    private fun presetMatchesStatus(
        preset: MarketPreset,
        activeId: String?,
        status: MarketStatus,
    ): Boolean = when (status) {
        MarketStatus.ALL, MarketStatus.AVAILABLE -> true
        MarketStatus.INSTALLED -> preset.id == activeId
        else -> false
    }

    private fun matches(q: String, plugin: UiMarketPlugin, kind: String): Boolean =
        q.isEmpty() || listOfNotNull(
            plugin.id,
            plugin.name,
            plugin.description,
            plugin.author,
            plugin.version,
            plugin.repo,
            plugin.tags?.joinToString(" "),
            kind,
        ).any { it.contains(q, ignoreCase = true) }

    private fun matches(q: String, item: PluginMarketViewModel.Item, kind: String): Boolean =
        matches(
            q,
            item.entry.name,
            item.entry.id,
            item.entry.description,
            item.entry.author,
            item.entry.version,
            item.entry.repo,
            item.entry.tags?.joinToString(" "),
            kind,
        )

    private fun matches(q: String, vararg values: String?): Boolean =
        q.isEmpty() || values.any { it?.contains(q, ignoreCase = true) == true }

    private fun matchesPreset(q: String, preset: MarketPreset): Boolean =
        matches(
            q,
            preset.id,
            preset.name,
            preset.description,
            preset.source,
            preset.author,
            preset.repo,
            preset.tags?.joinToString(" "),
            "整合包 preset",
        )

    private fun MarketItem.nameForSort(): String = when (this) {
        is MarketItem.Preset -> preset.name
        is MarketItem.Ui -> plugin.name
        is MarketItem.Dex -> item.entry.name
        is MarketItem.Builtin -> row.name
        is MarketItem.Apk -> plugin.name
        is MarketItem.Section -> id
    }
}
