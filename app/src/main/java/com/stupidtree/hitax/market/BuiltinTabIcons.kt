package com.stupidtree.hitax.market

import androidx.annotation.DrawableRes
import com.stupidtree.hitax.R

/**
 * 预设底栏 tab 引用**宿主内置图标**的解析。
 *
 * `PresetTab.iconAsset` 原本只表示「APK 插件内的图标资源路径」。为了复刻 HITA_Android 3.1.2
 * 的底栏图标（`ic_baseline_check_24` / `ic_baseline_toys_24` / `ic_nav_blog` 等），
 * 这里约定 `builtin:<name>` 前缀表示使用宿主自带的矢量图，`<name>` 见 [NAMES]。
 *
 * 解析不到时返回 null，由调用方回退到插件自带图标或通用图标——预设写了未知名字不该让界面崩。
 */
object BuiltinTabIcons {

    const val PREFIX = "builtin:"

    /** 内置图标名 → 宿主 drawable。名字是预设与宿主之间的契约，不要随意改名。 */
    private val NAMES: Map<String, Int> = linkedMapOf(
        // 3.1.2 HOMEWORK
        "check" to R.drawable.ic_baseline_check_24,
        // 3.1.2 TIMELINE
        "today" to R.drawable.ic_nav_today,
        // 3.1.2 TIMETABLE
        "timetable" to R.drawable.ic_nav_timetable,
        // 3.1.2 AGENT
        "toys" to R.drawable.ic_baseline_toys_24,
        // 3.1.2 BLOG
        "blog" to R.drawable.ic_nav_blog,
        // 3.1.2 NAVIGATION
        "navigation" to R.drawable.ic_nav_navigation,
        // 通用插件位
        "plugin" to R.drawable.ic_nav_plugin,
    )

    /** 该 `iconAsset` 是否是内置图标引用。 */
    fun isBuiltin(iconAsset: String?): Boolean = parseName(iconAsset) != null

    /**
     * 取出 `builtin:` 后面的名字；不是内置引用时返回 null。
     * 空名字视为非法（`"builtin:"` 返回 null）。
     */
    fun parseName(iconAsset: String?): String? {
        val raw = iconAsset?.trim().orEmpty()
        if (!raw.startsWith(PREFIX)) return null
        return raw.removePrefix(PREFIX).trim().ifBlank { null }
    }

    /** 解析为宿主 drawable；非内置引用或名字未知时返回 null。 */
    @DrawableRes
    fun resolve(iconAsset: String?): Int? = parseName(iconAsset)?.let(NAMES::get)

    /** 预设可用的名字集合，供文档与校验使用。 */
    fun available(): Set<String> = NAMES.keys
}
