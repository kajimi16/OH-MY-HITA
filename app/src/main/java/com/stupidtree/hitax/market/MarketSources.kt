package com.stupidtree.hitax.market

import android.content.Context
import com.google.gson.Gson
import com.stupidtree.hitax.BuildConfig

/** 一个插件源：name 用于展示，url 为空表示使用构建期内置索引。 */
data class MarketSource(val name: String, val url: String)

/**
 * 插件源与安装来源的持久化。
 *
 * 参考 AstrBot：默认源 + 用户自定义源；安装时记录来源，[PluginMarketViewModel.updateAll]
 * 会优先回到该来源解析新版本。
 */
object MarketSources {
    private const val SP = "market_sources"
    private const val KEY_CUSTOM = "custom"
    private const val KEY_ACTIVE = "active"
    private const val KEY_INSTALL = "installed_from"
    private val gson = Gson()

    fun defaultSource(): MarketSource = MarketSource(
        name = if (BuildConfig.PLUGIN_INDEX_URL.isBlank()) "内置" else "默认源",
        url = BuildConfig.PLUGIN_INDEX_URL,
    )

    fun all(context: Context): List<MarketSource> {
        val custom = runCatching {
            gson.fromJson<List<MarketSource>>(
                prefs(context).getString(KEY_CUSTOM, null),
                object : com.google.gson.reflect.TypeToken<List<MarketSource>>() {}.type,
            )
        }.getOrNull().orEmpty()
        return listOf(defaultSource()) + custom
    }

    fun active(context: Context): MarketSource {
        val all = all(context)
        val index = prefs(context).getInt(KEY_ACTIVE, 0)
        return all.getOrElse(index) { all.first() }
    }

    fun setActive(context: Context, source: MarketSource) {
        val index = all(context).indexOfFirst { it.url == source.url && it.name == source.name }
        if (index >= 0) prefs(context).edit().putInt(KEY_ACTIVE, index).apply()
    }

    /** @return 出错时返回错误信息，成功返回 null。 */
    fun add(context: Context, name: String, url: String): String? {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return "索引地址必须以 http(s):// 开头"
        }
        val custom = all(context).drop(1)
        if (custom.any { it.url == trimmed }) return "该插件源已存在"
        val next = custom + MarketSource(name.trim().ifBlank { hostOf(trimmed) }, trimmed)
        prefs(context).edit()
            .putString(KEY_CUSTOM, gson.toJson(next))
            .apply()
        return null
    }

    fun remove(context: Context, source: MarketSource) {
        val custom = all(context).drop(1).filterNot { it.url == source.url }
        prefs(context).edit()
            .putString(KEY_CUSTOM, gson.toJson(custom))
            .putInt(KEY_ACTIVE, 0)
            .apply()
    }

    fun recordInstall(context: Context, pluginId: String, sourceUrl: String) {
        val map = installMap(context).toMutableMap()
        map[pluginId] = sourceUrl
        prefs(context).edit()
            .putString(KEY_INSTALL, gson.toJson(map))
            .apply()
    }

    fun installSource(context: Context, pluginId: String): String? =
        installMap(context)[pluginId]

    private fun installMap(context: Context): Map<String, String> = runCatching {
        gson.fromJson<Map<String, String>>(
            prefs(context).getString(KEY_INSTALL, null),
            object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type,
        )
    }.getOrNull().orEmpty()

    private fun prefs(context: Context) =
        context.getSharedPreferences(SP, Context.MODE_PRIVATE)

    private fun hostOf(url: String): String =
        runCatching { java.net.URI(url).host }.getOrNull() ?: "自定义源"
}
