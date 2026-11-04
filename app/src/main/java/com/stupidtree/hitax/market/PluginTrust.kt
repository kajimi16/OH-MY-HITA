package com.stupidtree.hitax.market

import android.content.Context
import com.google.gson.JsonParser

/**
 * 插件信任模型。
 *
 * 默认所有插件都是「未验证」；只有与 OH-MY-HITA 一起核验过的半官方插件，
 * 才由维护者加入 `assets/verified_plugins.json` 的白名单。
 *
 * 未验证不等于不可用：安装/打开前会给用户明确提示，由用户自己决定。
 */
object PluginTrust {

    enum class Level { VERIFIED, UNVERIFIED }

    fun level(verifiedIds: Set<String>, id: String): Level =
        if (id in verifiedIds) Level.VERIFIED else Level.UNVERIFIED

    /** 解析白名单；格式非法时返回空集合（= 全部未验证）。 */
    fun parseVerifiedIds(json: String): Set<String> = runCatching {
        JsonParser.parseString(json).asJsonObject
            .getAsJsonArray("verified")
            .mapNotNull { it.asJsonObject.get("id")?.asString }
            .toSet()
    }.getOrDefault(emptySet())
}

class PluginTrustStore(context: Context) {

    private val verifiedIds: Set<String> = runCatching {
        context.assets.open("verified_plugins.json").bufferedReader().use {
            PluginTrust.parseVerifiedIds(it.readText())
        }
    }.getOrDefault(emptySet())

    fun level(id: String): PluginTrust.Level = PluginTrust.level(verifiedIds, id)

    fun isVerified(id: String): Boolean = level(id) == PluginTrust.Level.VERIFIED
}
