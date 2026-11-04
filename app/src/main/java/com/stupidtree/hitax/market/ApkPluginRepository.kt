package com.stupidtree.hitax.market

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.stupidtree.hita.api.HITA_API_VERSION
import com.stupidtree.hita.api.PluginApkProtocol

/**
 * 通过 PackageManager 发现已安装的独立 APK 插件。
 *
 * 只认声明了 [PluginApkProtocol.ACTION] 的入口 Activity；
 * 列表展示不构成信任，打开动作由用户触发。
 */
class ApkPluginRepository(private val context: Context) {

    fun discover(): List<ApkPlugin> {
        val pm = context.packageManager
        val intents = pm.queryIntentActivities(Intent(PluginApkProtocol.ACTION), PackageManager.GET_META_DATA)
        return intents.mapNotNull { info ->
            val appInfo = info.activityInfo?.applicationInfo ?: return@mapNotNull null
            val meta = appInfo.metaData ?: return@mapNotNull null
            val id = meta.getString(PluginApkProtocol.META_ID) ?: return@mapNotNull null
            val api = meta.getInt(PluginApkProtocol.META_API_VERSION, 1)
            val versionName = runCatching {
                pm.getPackageInfo(appInfo.packageName, 0).versionName
            }.getOrNull() ?: "?"
            ApkPlugin(
                id = id,
                packageName = appInfo.packageName,
                name = appInfo.loadLabel(pm).toString(),
                versionName = versionName,
                apiVersion = api,
                entry = android.content.ComponentName(appInfo.packageName, info.activityInfo.name),
            )
        }.distinctBy { it.packageName }
    }

    fun supported(plugin: ApkPlugin): Boolean =
        ApkPluginCompat.isSupported(plugin.apiVersion, HITA_API_VERSION)
}
