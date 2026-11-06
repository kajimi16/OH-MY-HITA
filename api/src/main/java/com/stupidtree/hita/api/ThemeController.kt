package com.stupidtree.hita.api

/**
 * 宿主提供给插件的主题控制能力。
 *
 * 方向是**宿主 → 插件**：插件通过 `ctx.service(ThemeController.KEY)` 取得，
 * 由宿主实现（当前实现是 `AppCompatDelegate.setDefaultNightMode`）。
 */
interface ThemeController {

    fun setNightMode(night: Boolean)

    /** 配色预设：classic / fresh / focus / high_contrast；非法值返回 false。 */
    fun setColorPreset(preset: String): Boolean

    fun colorPreset(): String

    /** 主题风格：classic / apple_glass / cyber / sora_cloud / p5 / deep_space / sumi。 */
    fun setThemeStyle(style: String): Boolean

    fun themeStyle(): String

    /** 字体缩放 0.85–1.30；下个界面生效。 */
    fun setFontScale(scale: Float): Boolean

    fun fontScale(): Float

    companion object {
        val KEY = ServiceKey<ThemeController>("hita.theme")
    }
}
