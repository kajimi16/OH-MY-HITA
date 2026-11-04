package com.stupidtree.hitax.plugin

import com.google.gson.JsonParser

/**
 * 构建期插件清单的一条记录。
 *
 * 宿主只按类名反射加载，不 import 任何插件模块，因此删掉插件依赖仍能编译。
 */
data class PluginEntry(
    val id: String,
    val className: String,
    val needsApplication: Boolean,
    /** 市场里展示用；缺省回落到 id。 */
    val displayName: String = id,
)

object PluginEntries {

    fun parse(json: String): List<PluginEntry> = runCatching {
        val root = JsonParser.parseString(json).asJsonObject
        val enabled = root.getAsJsonArray("enabled")?.mapNotNull { it.asString }?.toSet().orEmpty()
        root.getAsJsonArray("plugins").mapNotNull { element ->
            val obj = element.asJsonObject
            val id = obj.get("id")?.asString ?: return@mapNotNull null
            if (enabled.isNotEmpty() && id !in enabled) return@mapNotNull null
            PluginEntry(
                id = id,
                className = obj.get("class")?.asString ?: return@mapNotNull null,
                needsApplication = obj.get("needsApplication")?.asBoolean ?: false,
                displayName = obj.get("name")?.asString ?: id,
            )
        }
    }.getOrDefault(emptyList())
}
