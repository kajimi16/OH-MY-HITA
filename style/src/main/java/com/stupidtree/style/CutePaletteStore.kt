package com.stupidtree.style

import android.content.Context

/** HITA_CUTE 的课表配色方案。 */
object CutePaletteStore {
    private const val PREFS = "cute_appearance"
    private const val KEY_PALETTE = "course_palette"

    data class Palette(val id: String, val label: String, val colors: List<String>)

    val options = listOf(
        Palette(
            "material", "Material",
            listOf("#ec407a", "#FF9E00", "#7c4dff", "#536dfe", "#2196f3", "#26c6da", "#009688", "#7cba59", "#E96D71"),
        ),
        Palette(
            "pastel", "Pastel",
            listOf("#F8BBD0", "#FFCCBC", "#E1BEE7", "#C5CAE9", "#BBDEFB", "#B2EBF2", "#B2DFDB", "#DCEDC8", "#FFF9C4"),
        ),
        Palette(
            "macaron", "Macaron",
            listOf("#FF9AA2", "#FFB7B2", "#B5EAD7", "#C7CEEA", "#FFDAC1", "#E2F0CB", "#A0E7E5", "#FFD1DC", "#DAEAF0"),
        ),
        Palette(
            "nature", "Nature",
            listOf("#A8E6CF", "#DCEDC1", "#FFD3B6", "#FFAAA5", "#FF8B94", "#B5EAD7", "#C7CEEA", "#E2F0CB", "#97D2C1"),
        ),
        Palette(
            "ocean", "Ocean",
            listOf("#004D7A", "#008793", "#00BFA5", "#26A69A", "#4DB6AC", "#80CBC4", "#B2DFDB", "#00695C", "#00796B"),
        ),
        Palette(
            "sunset", "Sunset",
            listOf("#FF6F61", "#FF8A65", "#FFAB91", "#FFCCBC", "#E91E63", "#F06292", "#F48FB1", "#FFB74D", "#FF8A80"),
        ),
    )

    fun activeId(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PALETTE, "material") ?: "material"

    fun setActiveId(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PALETTE, id).apply()
    }

    fun active(context: Context): Palette =
        options.firstOrNull { it.id == activeId(context) } ?: options.first()

    fun colors(context: Context): List<String> = active(context).colors

    /** 配色只在 HITA_CUTE 预设内生效；其它预设回退到 Material。 */
    fun colorsForActivePreset(context: Context): List<String> {
        val preset = context.getSharedPreferences("preset_state", Context.MODE_PRIVATE)
            .getString("active_preset", null)
        return if (preset == "hita-cute-1.6.0") colors(context) else options.first().colors
    }
}
