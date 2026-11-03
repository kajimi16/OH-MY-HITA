package com.stupidtree.hitax.plugin

import android.app.Application
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.theta.ThetaPlugin

/**
 * 构建期插件清单 —— profile.yml 的雏形。
 *
 * 这里是**唯一**决定「这个 APK 装配哪些插件」的地方：删掉一行，对应功能就不再被装载，
 * 宿主无需改动。注意它只是「不装配」——模块仍会编译并打进 APK；
 * 要真正从产物里移除，还需删掉 app/build.gradle 的模块依赖与 settings.gradle 的 include。
 *
 * 插件通过构造函数拿 Android 依赖（如 Context），内核契约保持纯 Kotlin。
 */
object PluginCatalog {

    fun plugins(application: Application): List<Plugin> {
        val all = listOf(
            ThetaPlugin(),
            HitaPlugin(application),
        )
        return PluginComposition.select(all, readEnabledIds(application))
    }

    /** `assets/plugins.json` 的 enabled 列表；文件缺失或解析失败时返回 null（表示全部启用）。 */
    private fun readEnabledIds(application: Application): Set<String>? = runCatching {
        val text = application.assets.open("plugins.json").bufferedReader().use { it.readText() }
        val root = com.google.gson.JsonParser.parseString(text).asJsonObject
        val array = root.getAsJsonArray("enabled") ?: return null
        array.mapNotNull { it.asString }.toSet()
    }.getOrNull()
}
