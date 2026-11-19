package com.stupidtree.hitax.market

import android.content.Context

/**
 * 当前生效整合包声明的主界面布局（底栏 tab 列表）。
 *
 * 新数据统一写入 [PresetStateStore]，与 active preset / shell 同一个 SharedPreferences
 * 提交，避免冷启动读到“active 已换、tabs 未换”的半状态。旧版本的
 * `preset_layout` 文件仍会在首次读取时迁移。
 */
object PresetLayoutStore {
    private const val LEGACY_PREFS = "preset_layout"
    private const val LEGACY_PRESET = "preset_id"
    private const val LEGACY_TABS = "tabs_json"

    fun save(context: Context, presetId: String, tabs: List<PresetTab>?) {
        PresetStateStore.saveLayout(context, presetId, tabs)
    }

    fun tabs(context: Context): List<PresetTab> {
        // 新布局一旦写过（即使 tabs 为空，例如 HITA-L），就不能再回退到旧文件；
        // 否则旧整合包的 6 个 tab 会在冷启动时被“复活”。
        if (PresetStateStore.layoutPresetId(context) != null) {
            return PresetStateStore.tabs(context)
        }
        val legacy = legacyTabs(context)
        if (legacy.isEmpty()) return emptyList()
        PresetStateStore.saveLayout(context, legacyPresetId(context) ?: "", legacy)
        return legacy
    }

    private fun legacyTabs(context: Context): List<PresetTab> =
        PresetLayoutCodec.decode(
            context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
                .getString(LEGACY_TABS, null),
        )

    private fun legacyPresetId(context: Context): String? =
        context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
            .getString(LEGACY_PRESET, null)
}
