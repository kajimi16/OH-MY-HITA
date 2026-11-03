package com.stupidtree.hitax.kernel

import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.theta.ThetaPlugin

/**
 * 构建期插件清单 —— profile.yml 的雏形。
 *
 * 这里是**唯一**决定「这个 APK 由哪些插件组成」的地方：
 * 删掉一行，对应功能就不参与构建；增删功能不再需要改动 UI 或 Application。
 */
object PluginCatalog {

    fun plugins(): List<Plugin> = listOf(
        ThetaPlugin(),
    )
}
