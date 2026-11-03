package com.stupidtree.hitax.kernel

import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.theta.ThetaPlugin

/**
 * 构建期插件清单 —— profile.yml 的雏形。
 *
 * 这里是**唯一**决定「这个 APK 装配哪些插件」的地方：删掉一行，对应功能就不再被装载，
 * 宿主无需改动。注意它只是「不装配」——模块仍会编译并打进了 APK；
 * 要真正从产物里移除，还需删掉 app/build.gradle 的模块依赖与 settings.gradle 的 include。
 */
object PluginCatalog {

    fun plugins(): List<Plugin> = listOf(
        ThetaPlugin(),
    )
}
