package com.stupidtree.hitax.plugin

import com.google.gson.JsonParser
import com.stupidtree.hita.kernel.PluginArtifactMeta
import com.stupidtree.hita.kernel.PluginDescriptor
import com.stupidtree.hita.kernel.PluginOrigin
import com.stupidtree.hitax.BuildConfig

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
    /** 兼容性元数据；旧清单缺省时按旧行为处理。 */
    val apiVersion: Int? = null,
    val minHostApi: Int? = null,
    val maxHostApi: Int? = null,
    val apiAbi: String? = null,
    val minSdk: Int? = null,
    val version: String? = null,
    val requires: Set<String> = emptySet(),
    val provides: Set<String> = emptySet(),
) {
    fun artifactMeta(): PluginArtifactMeta = PluginArtifactMeta(
        id = id,
        requiresApi = apiVersion ?: 1,
        minHostApi = minHostApi,
        maxHostApi = maxHostApi,
        apiAbi = apiAbi,
        minSdk = minSdk,
        version = version,
    )

    fun descriptor(): PluginDescriptor = PluginDescriptor(
        id = id,
        requires = requires,
        provides = provides,
        origin = PluginOrigin.BUILTIN,
        // 内置插件随 APK 发布，清单没写版本时用宿主版本，保证诊断里版本可读。
        version = version ?: BuildConfig.VERSION_NAME,
    )
}

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
                apiVersion = obj.get("apiVersion")?.asInt,
                minHostApi = obj.get("minHostApi")?.asInt,
                maxHostApi = obj.get("maxHostApi")?.asInt,
                apiAbi = obj.get("apiAbi")?.asString,
                minSdk = obj.get("minSdk")?.asInt,
                version = obj.get("version")?.asString,
                requires = obj.getAsJsonArray("requires")
                    ?.mapNotNull { it.asString }?.toSet().orEmpty(),
                provides = obj.getAsJsonArray("provides")
                    ?.mapNotNull { it.asString }?.toSet().orEmpty(),
            )
        }
    }.getOrDefault(emptyList())
}
