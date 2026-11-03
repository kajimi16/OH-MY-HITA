package com.stupidtree.hita.api

/**
 * 宿主提供给插件的主题控制能力。
 *
 * 方向是**宿主 → 插件**：插件通过 `ctx.service(ThemeController.KEY)` 取得，
 * 由宿主实现（当前实现是 `AppCompatDelegate.setDefaultNightMode`）。
 */
interface ThemeController {

    fun setNightMode(night: Boolean)

    companion object {
        val KEY = ServiceKey<ThemeController>("hita.theme")
    }
}
