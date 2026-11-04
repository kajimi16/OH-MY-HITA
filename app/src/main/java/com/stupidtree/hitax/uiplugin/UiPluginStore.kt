package com.stupidtree.hitax.uiplugin

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** 界面插件安装记录（预fs 里的 JSON；插件文件本身在 filesDir/ui-plugins）。 */
object UiPluginStore {
    private const val SP = "ui_plugins"
    private const val KEY = "installed"
    private val gson = Gson()
    private val type = object : TypeToken<List<InstalledUiPlugin>>() {}.type

    fun list(context: Context): List<InstalledUiPlugin> {
        val text = context.getSharedPreferences(SP, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyList()
        return runCatching { gson.fromJson<List<InstalledUiPlugin>>(text, type) }
            .getOrDefault(emptyList())
    }

    fun find(context: Context, id: String): InstalledUiPlugin? =
        list(context).firstOrNull { it.id == id }

    fun put(context: Context, plugin: InstalledUiPlugin) {
        val next = list(context).filterNot { it.id == plugin.id } + plugin
        save(context, next)
    }

    fun remove(context: Context, id: String) {
        save(context, list(context).filterNot { it.id == id })
    }

    private fun save(context: Context, plugins: List<InstalledUiPlugin>) {
        context.getSharedPreferences(SP, Context.MODE_PRIVATE)
            .edit().putString(KEY, gson.toJson(plugins, type)).apply()
    }
}
