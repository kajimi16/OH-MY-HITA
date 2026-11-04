package com.stupidtree.hitax.plugin

import android.app.Application
import android.content.Context
import android.util.Log

/**
 * 「卸载时清除数据」的延迟执行队列。
 *
 * 卸载发生在 UI 存活、Room/ViewModel 仍可能被访问的时刻，此时删库会被立刻重建。
 * 因此卸载只写标记，真正的删除放到下一次进程启动、插件装配之前执行。
 */
object PluginPurge {
    private const val SP = "plugin_purge"
    private const val KEY = "pending_ids"
    private const val TAG = "PluginPurge"

    fun pending(context: Context): Set<String> =
        context.getSharedPreferences(SP, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())?.toSet() ?: emptySet()

    fun mark(context: Context, pluginId: String) {
        val prefs = context.getSharedPreferences(SP, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY, pending(context) + pluginId).apply()
    }

    /** 用户重新启用插件时取消待清理，避免下次冷启动误删新数据。 */
    fun cancel(context: Context, pluginId: String) = clear(context, pluginId)

    private fun clear(context: Context, pluginId: String) {
        val prefs = context.getSharedPreferences(SP, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY, pending(context) - pluginId).apply()
    }

    /**
     * 卸载后延迟执行一次。
     *
     * 此时插件已热卸载、界面已重建，不会再有 Room/ViewModel 访问；
     * 若仍失败，标记保留，下次冷启动再兜底。
     */
    fun scheduleNow(application: Application, pluginId: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val entry = PluginCatalog.entry(application, pluginId) ?: return@postDelayed
            val plugin = PluginCatalog.instantiate(entry, application) ?: return@postDelayed
            runCatching { plugin.purgeData() }
                .onSuccess { clear(application, pluginId) }
                .onFailure { Log.e(TAG, "delayed purge failed: $pluginId", it) }
        }, 1200)
    }

    /** 在 Application.onCreate、任何插件装配之前调用。 */
    fun runPending(application: Application) {
        pending(application).forEach { pluginId ->
            val entry = PluginCatalog.entry(application, pluginId)
            if (entry == null) {
                clear(application, pluginId)
                return@forEach
            }
            val plugin = PluginCatalog.instantiate(entry, application)
            if (plugin == null) {
                Log.w(TAG, "无法实例化 $pluginId，保留待清理标记")
                return@forEach
            }
            val ok = runCatching { plugin.purgeData() }
                .onFailure { Log.e(TAG, "purge failed: $pluginId", it) }
                .isSuccess
            if (ok) clear(application, pluginId)
        }
    }
}
