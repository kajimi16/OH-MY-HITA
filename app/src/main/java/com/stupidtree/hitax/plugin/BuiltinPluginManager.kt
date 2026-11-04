package com.stupidtree.hitax.plugin

import android.app.Application
import com.stupidtree.hitax.HApplication

/**
 * 内置插件的「卸载 / 重新安装」。
 *
 * Android 不允许运行时删除 APK 里的 dex，所以这里的卸载是：
 * 1. 从内核热卸载（撤销服务、tab、导航项、同步表）；
 * 2. 禁用该插件的 Manifest 组件（桌面小组件、Activity、Service）；
 * 3. 持久化禁用标记，下次启动不再装配；
 * 4. 可选让插件自行清理数据库 / 偏好 / 定时任务。
 *
 * 代码仍留在 APK 中，体积不变——这是与「独立 APK 插件」的本质区别。
 */
class BuiltinPluginManager(private val application: Application) {

    data class Row(
        val id: String,
        val name: String,
        val installed: Boolean,
    )

    private val host get() = (application as HApplication).pluginHost

    fun rows(): List<Row> = PluginCatalog.entries(application).map {
        Row(id = it.id, name = it.displayName, installed = host.isInstalled(it.id))
    }

    /**
     * @param purge 为真时让插件清理自己的数据（数据库、偏好、定时任务）。
     */
    fun uninstall(pluginId: String, purge: Boolean): Result<Unit> = runCatching {
        PluginComponents.setEnabled(application, pluginId, false)
        if (purge) {
            // 先写标记（进程被杀也不丢），卸载完成、界面重建后再真正删数据
            PluginPurge.mark(application, pluginId)
            host.uninstall(pluginId)
            PluginPurge.scheduleNow(application, pluginId)
        } else {
            host.uninstall(pluginId)
        }
        PluginPrefs.setDisabled(application, pluginId, true)
    }

    fun install(pluginId: String): Result<Unit> = runCatching {
        val entry = PluginCatalog.entry(application, pluginId)
            ?: error("插件清单中没有 $pluginId")
        val plugin = PluginCatalog.instantiate(entry, application)
            ?: error("插件类不可用：${entry.className}")
        PluginPurge.cancel(application, pluginId)
        PluginPrefs.setDisabled(application, pluginId, false)
        host.install(plugin)
        check(host.isInstalled(pluginId)) { "插件安装失败：$pluginId" }
        PluginComponents.setEnabled(application, pluginId, true)
    }
}
