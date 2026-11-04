package com.stupidtree.hitax.market

import android.content.ComponentName

/** 一个已安装的独立 APK 插件。 */
data class ApkPlugin(
    val id: String,
    val packageName: String,
    val name: String,
    val versionName: String,
    val apiVersion: Int,
    val entry: ComponentName,
)

/** 兼容性判断，单独抽出来便于测试。 */
object ApkPluginCompat {
    fun isSupported(pluginApiVersion: Int, hostApiVersion: Int): Boolean =
        pluginApiVersion <= hostApiVersion
}
