package com.stupidtree.style

import android.app.Activity
import android.content.Context
import android.util.Log

object ThemeTools {
    enum class MODE { DARK, LIGHT, FOLLOW }

    enum class STYLE { CLASSIC, APPLE_GLASS, CYBER, SORA_CLOUD, P5, DEEP_SPACE, SUMI }

    val COLOR_PRESETS = listOf("classic", "fresh", "focus", "high_contrast")
    private const val KEY_COLOR_PRESET = "color_preset"
    private const val KEY_FONT_SCALE = "font_scale"
    private const val KEY_STYLE = "style"
    private const val DEFAULT_FONT_SCALE = 1.0f

    fun getColorPreset(context: Context): String {
        val value = context.getSharedPreferences("theme", Context.MODE_PRIVATE)
            .getString(KEY_COLOR_PRESET, "classic") ?: "classic"
        return if (value in COLOR_PRESETS) value else "classic"
    }

    fun setColorPreset(context: Context, preset: String): Boolean {
        if (preset !in COLOR_PRESETS) return false
        context.getSharedPreferences("theme", Context.MODE_PRIVATE).edit()
            .putString(KEY_COLOR_PRESET, preset).apply()
        return true
    }

    fun getThemeStyle(context: Context): STYLE {
        val value = context.getSharedPreferences("theme", Context.MODE_PRIVATE)
            .getString(KEY_STYLE, "classic") ?: "classic"
        return when (value) {
            "apple_glass" -> STYLE.APPLE_GLASS
            "cyber" -> STYLE.CYBER
            "sora_cloud" -> STYLE.SORA_CLOUD
            "p5" -> STYLE.P5
            "deep_space" -> STYLE.DEEP_SPACE
            "sumi" -> STYLE.SUMI
            else -> STYLE.CLASSIC
        }
    }

    fun setThemeStyle(context: Context, style: STYLE): Boolean {
        val value = when (style) {
            STYLE.CLASSIC -> "classic"
            STYLE.APPLE_GLASS -> "apple_glass"
            STYLE.CYBER -> "cyber"
            STYLE.SORA_CLOUD -> "sora_cloud"
            STYLE.P5 -> "p5"
            STYLE.DEEP_SPACE -> "deep_space"
            STYLE.SUMI -> "sumi"
        }
        context.getSharedPreferences("theme", Context.MODE_PRIVATE).edit()
            .putString(KEY_STYLE, value).apply()
        return true
    }

    fun getFontScale(context: Context): Float =
        context.getSharedPreferences("theme", Context.MODE_PRIVATE)
            .getFloat(KEY_FONT_SCALE, DEFAULT_FONT_SCALE)
            .coerceIn(0.85f, 1.30f)

    fun setFontScale(context: Context, scale: Float): Boolean {
        if (scale !in 0.85f..1.30f) return false
        context.getSharedPreferences("theme", Context.MODE_PRIVATE).edit()
            .putFloat(KEY_FONT_SCALE, scale).apply()
        return true
    }


    fun getThemeMode(context: Context): MODE {
        val pr = context.getSharedPreferences("theme", Context.MODE_PRIVATE)
        return when (pr.getString("mode", "follow")) {
            "dark" -> MODE.DARK
            "light" -> MODE.LIGHT
            else -> MODE.FOLLOW
        }
    }

    /** 供宿主能力（如插件契约）切换主题模式。 */
    fun setThemeMode(context: Context, mode: MODE) {
        val pr = context.getSharedPreferences("theme", Context.MODE_PRIVATE)
        val str = when (mode) {
            MODE.DARK -> "dark"
            MODE.LIGHT -> "light"
            else -> "follow"
        }
        pr.edit().putString("mode", str).apply()
    }

    fun switchTheme(activity:Activity){
        val newMode = when(getThemeMode(activity)){
            MODE.DARK->MODE.LIGHT
            MODE.LIGHT->MODE.FOLLOW
            MODE.FOLLOW->MODE.DARK
        }
        setThemeMode(activity,newMode)
        activity.recreate()
    }
}