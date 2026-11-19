package com.stupidtree.hitax.market

import android.content.Context

/**
 * 当前生效整合包及其界面形态。
 *
 * active preset、shell 与底栏 tabs 必须一起提交；分开写会出现
 * “active 已切换但底栏还是旧组合”的半状态，冷启动后表现为切换不生效。
 */
object PresetStateStore {
    private const val PREFS = "preset_state"
    private const val KEY_ACTIVE = "active_preset"
    private const val KEY_SHELL = "active_shell"
    private const val KEY_LAYOUT_PRESET = "layout_preset"
    private const val KEY_TABS = "tabs_json"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun activeId(context: Context): String? =
        prefs(context).getString(KEY_ACTIVE, null)

    fun activeShell(context: Context): String? {
        val stored = prefs(context).getString(KEY_SHELL, null)
        if (!stored.isNullOrBlank()) return stored
        return shellForPreset(activeId(context), null)
    }

    /**
     * 线上 HITA_Android 预设暂时没有声明 shell；由宿主提供稳定映射，
     * 避免“整合包已切换但仍走 MainActivity”的假切换。
     */
    fun shellForPreset(id: String?, declared: String?): String? {
        if (!declared.isNullOrBlank()) return declared
        return when (id) {
            "hita-android-3.1.2" -> "hita-android-3.1.2"
            else -> null
        }
    }

    fun layoutPresetId(context: Context): String? =
        prefs(context).getString(KEY_LAYOUT_PRESET, null)

    fun tabs(context: Context): List<PresetTab> =
        PresetLayoutCodec.decode(prefs(context).getString(KEY_TABS, null))

    /**
     * 一次性提交 active preset、shell 与 tabs。
     *
     * @return SharedPreferences 是否成功落盘；false 时调用方不能把新组合当作已生效。
     */
    fun saveUi(
        context: Context,
        id: String,
        shell: String?,
        tabs: List<PresetTab>?,
    ): Boolean {
        val tabsJson = PresetLayoutCodec.encode(tabs)
        val editor = prefs(context).edit()
            .putString(KEY_ACTIVE, id)
            .putString(KEY_LAYOUT_PRESET, id)
        if (shell.isNullOrBlank()) editor.remove(KEY_SHELL) else editor.putString(KEY_SHELL, shell)
        if (tabsJson == null) editor.remove(KEY_TABS) else editor.putString(KEY_TABS, tabsJson)
        return editor.commit()
    }

    /** 只更新布局，供存量预设补写 tabs 使用；不会改动 active preset。 */
    fun saveLayout(context: Context, presetId: String, tabs: List<PresetTab>?): Boolean {
        val tabsJson = PresetLayoutCodec.encode(tabs)
        val editor = prefs(context).edit().putString(KEY_LAYOUT_PRESET, presetId)
        if (tabsJson == null) editor.remove(KEY_TABS) else editor.putString(KEY_TABS, tabsJson)
        return editor.commit()
    }
}
