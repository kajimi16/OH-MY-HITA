package com.stupidtree.style

import android.content.Context
import android.graphics.Color

/** HITA_CUTE 的 20 套主题色；styleName 为 app 合并资源后的 style 名。 */
object CuteThemeStore {
    private const val PREFS = "cute_appearance"
    private const val KEY_THEME = "theme_color"

    data class Option(
        val id: String,
        val label: String,
        val colorHex: String,
        val styleName: String? = null,
    )

    val options = listOf(
        Option("blue", "晴空蓝", "#304FFE", null),
        Option("steel", "钢蓝", "#5C7FA0", "AppTheme_CuteSteel"),
        Option("sky", "天空蓝", "#5A9BB8", "AppTheme_CuteSky"),
        Option("teal", "水鸭青", "#4D9088", "AppTheme_CuteTeal"),
        Option("mint", "薄荷", "#5E9B8C", "AppTheme_CuteMint"),
        Option("sage", "鼠尾草", "#6E9B72", "AppTheme_CuteSage"),
        Option("olive", "橄榄", "#8E9A5E", "AppTheme_CuteOlive"),
        Option("amber", "琥珀", "#B88A50", "AppTheme_CuteAmber"),
        Option("terracotta", "陶土", "#C07850", "AppTheme_CuteTerracotta"),
        Option("coral", "珊瑚", "#C07060", "AppTheme_CuteCoral"),
        Option("rose", "玫瑰", "#B06070", "AppTheme_CuteRose"),
        Option("mauve", "藕紫", "#9B6888", "AppTheme_CuteMauve"),
        Option("plum", "梅子", "#806890", "AppTheme_CutePlum"),
        Option("violet", "紫罗兰", "#7068A0", "AppTheme_CuteViolet"),
        Option("indigo", "靛蓝", "#5C6098", "AppTheme_CuteIndigo"),
        Option("slate", "石板", "#687890", "AppTheme_CuteSlate"),
        Option("taupe", "灰褐", "#82746B", "AppTheme_CuteTaupe"),
        Option("wine", "酒红", "#883850", "AppTheme_CuteWine"),
        Option("bronze", "古铜", "#907050", "AppTheme_CuteBronze"),
        Option("charcoal", "炭灰", "#586068", "AppTheme_CuteCharcoal"),
    )

    fun activeId(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, "blue") ?: "blue"

    fun setActiveId(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_THEME, id).apply()
    }

    fun active(context: Context): Option =
        options.firstOrNull { it.id == activeId(context) } ?: options.first()

    fun activeStyleName(context: Context): String? = active(context).styleName

    fun findClosest(color: Int): Option {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return options.minByOrNull { option ->
            val c = Color.parseColor(option.colorHex)
            val dr = r - ((c shr 16) and 0xFF)
            val dg = g - ((c shr 8) and 0xFF)
            val db = b - (c and 0xFF)
            2 * dr * dr + 4 * dg * dg + 3 * db * db
        } ?: options.first()
    }
}
