package com.stupidtree.hitax.plugin

import android.content.Context
import com.google.gson.JsonParser

/**
 * `assets/plugin_exemptions.json` 的精确豁免表。
 *
 * 只按 `id@version` 精确匹配，不继承插件升级或宿主升级；文件缺失/损坏时视为无豁免。
 */
object PluginExemptions {

    fun isExempt(
        context: Context,
        pluginId: String,
        pluginVersion: String?,
        hostVersion: Int,
    ): Boolean {
        if (pluginVersion.isNullOrBlank()) return false
        return runCatching {
            val text = context.assets.open("plugin_exemptions.json")
                .bufferedReader().use { it.readText() }
            val entries = JsonParser.parseString(text).asJsonObject
                .getAsJsonArray("exemptions") ?: return@runCatching false
            entries.any { element ->
                val obj = element.asJsonObject
                obj.get("id")?.asString == pluginId &&
                    obj.get("version")?.asString == pluginVersion &&
                    obj.getAsJsonArray("hostVersions")
                        ?.mapNotNull { it.asInt }?.contains(hostVersion) == true
            }
        }.getOrDefault(false)
    }
}
