package com.stupidtree.hitax.plugin

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * 内置插件声明的 Manifest 组件（Activity / Receiver / Service）。
 *
 * 卸载插件时禁用它们，否则桌面小组件列表里仍会出现一个永远加载失败的项；
 * 重新安装时再启用。按类名前缀匹配，避免在宿主里逐个硬编码组件全名。
 */
object PluginComponents {

    private val prefixes: Map<String, List<String>> = mapOf(
        "com.stupidtree.hitax.timetable" to listOf(
            "com.stupidtree.hitax.ui.timetable.",
            "com.stupidtree.hitax.ui.subject.",
            "com.stupidtree.hitax.ui.widgets.today.",
        ),
        "com.stupidtree.hitax.eas" to listOf("com.stupidtree.hitax.ui.eas."),
        "com.stupidtree.hitax.resource" to listOf("com.stupidtree.hitax.ui.resource."),
        "com.stupidtree.hitax.user" to listOf(
            "com.stupidtree.hitax.ui.profile.",
            "com.stupidtree.hitax.ui.myprofile.",
        ),
        "com.stupidtree.hita.theta" to listOf("com.stupidtree.hita.theta."),
    )

    fun setEnabled(context: Context, pluginId: String, enabled: Boolean) {
        val match = prefixes[pluginId] ?: return
        val pm = context.packageManager
        val pkg = context.packageName
        val state = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        // 必须带 MATCH_DISABLED_COMPONENTS：否则已被禁用的组件读不到，无法重新启用
        val flags = PackageManager.GET_ACTIVITIES or
            PackageManager.GET_RECEIVERS or
            PackageManager.GET_SERVICES or
            PackageManager.MATCH_DISABLED_COMPONENTS
        val info = runCatching {
            pm.getPackageInfo(pkg, flags)
        }.onFailure { Log.e("PluginComponents", "getPackageInfo failed", it) }.getOrNull() ?: return

        val names = ArrayList<String>()
        info.activities?.forEach { it.name?.let(names::add) }
        info.receivers?.forEach { it.name?.let(names::add) }
        info.services?.forEach { it.name?.let(names::add) }

        names.distinct()
            .filter { full -> match.any { full.startsWith(it) } }
            .forEach { full ->
                val component = ComponentName(pkg, full)
                if (pm.getComponentEnabledSetting(component) != state) {
                    runCatching {
                        pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
                    }.onFailure {
                        Log.e("PluginComponents", "toggle failed: $full", it)
                    }
                }
            }
    }
}
