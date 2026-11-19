package com.stupidtree.hitax.plugin

import android.app.Application
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.market.CompositionKind
import com.stupidtree.hitax.market.CompositionStore

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

    /** 开关语义：启用=安装，停用=卸载但保留数据。 */
    fun setEnabled(pluginId: String, enabled: Boolean): Result<Unit> =
        if (enabled) install(pluginId) else uninstall(pluginId, purge = false)

    /**
     * 启动期组合重放专用：只改运行时装配/组件，不写 `PluginPrefs`。
     *
     * 这样旧 preset 的停用不会污染用户意图；下一次冷启动仍由 installAll 重建，
     * 再由 startup replay 重新应用当前组合。
     */
    fun setEnabledTransient(pluginId: String, enabled: Boolean): Result<Unit> = runCatching {
        check(!CompositionStore.isKillSwitched(application, CompositionKind.BUILTIN, pluginId)) {
            "插件已被宿主安全开关停用"
        }
        if (enabled) {
            check(!PluginPrefs.isDisabled(application, pluginId)) {
                "插件被用户停用"
            }
            if (!host.isInstalled(pluginId)) {
                val entry = PluginCatalog.entry(application, pluginId)
                    ?: error("插件清单中没有 $pluginId")
                val plugin = PluginCatalog.instantiate(entry, application)
                    ?: error("插件类不可用：${entry.className}")
                PluginPurge.cancel(application, pluginId)
                host.install(plugin, entry.descriptor())
            }
            check(host.isInstalled(pluginId)) { "插件安装失败：$pluginId" }
            PluginComponents.setEnabled(application, pluginId, true)
        } else {
            host.uninstall(pluginId)
            PluginComponents.setEnabled(application, pluginId, false)
        }
    }

    fun install(pluginId: String): Result<Unit> = runCatching {
        check(!CompositionStore.isKillSwitched(application, CompositionKind.BUILTIN, pluginId)) {
            "插件已被宿主安全开关停用"
        }
        val entry = PluginCatalog.entry(application, pluginId)
            ?: error("插件清单中没有 $pluginId")
        val plugin = PluginCatalog.instantiate(entry, application)
            ?: error("插件类不可用：${entry.className}")
        PluginPurge.cancel(application, pluginId)
        PluginPrefs.setDisabled(application, pluginId, false)
        host.install(plugin, entry.descriptor())
        check(host.isInstalled(pluginId) || host.isWaiting(pluginId)) {
            "插件安装失败：$pluginId"
        }
        PluginComponents.setEnabled(application, pluginId, true)
    }
}
