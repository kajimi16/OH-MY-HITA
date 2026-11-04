package com.stupidtree.hitax.plugin

import android.app.Application
import android.util.Log
import com.stupidtree.hita.api.Plugin

/**
 * 构建期插件清单 —— profile 的雏形。
 *
 * 只读 `assets/plugins.json` 里的类名并反射实例化，**不 import 任何插件模块**：
 * 删除 `:plugin-hita` / `:theta` 的依赖后，宿主仍能编译并空壳启动。
 */
object PluginCatalog {

    fun plugins(application: Application): List<Plugin> {
        val entries = runCatching {
            val text = application.assets.open("plugins.json").bufferedReader().use { it.readText() }
            PluginEntries.parse(text)
        }.getOrElse {
            Log.e(TAG, "读取 plugins.json 失败", it)
            emptyList()
        }

        return entries.mapNotNull { entry -> load(entry, application) }
    }

    private fun load(entry: PluginEntry, application: Application): Plugin? = runCatching {
        val clazz = Class.forName(entry.className, true, PluginCatalog::class.java.classLoader)
        if (entry.needsApplication) {
            clazz.getConstructor(Application::class.java).newInstance(application) as Plugin
        } else {
            clazz.getConstructor().newInstance() as Plugin
        }
    }.onFailure {
        Log.e(TAG, "插件不可用（可能未打包）：${entry.className}", it)
    }.getOrNull()

    private const val TAG = "PluginCatalog"
}
