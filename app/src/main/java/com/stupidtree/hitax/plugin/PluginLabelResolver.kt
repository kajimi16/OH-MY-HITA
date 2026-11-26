package com.stupidtree.hitax.plugin

import android.content.Context
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.LocalizedComposeUiPlugin
import com.stupidtree.hita.api.NavEntry
import com.stupidtree.hita.api.SearchTab
import com.stupidtree.hita.api.TabContribution
import com.stupidtree.hita.api.TextNavEntry
import com.stupidtree.hita.api.ViewSearchTab

/**
 * 宿主解析插件贡献点标题。
 *
 * 顺序：`titleProvider(context)` → `textTitle` / 字符串标题 → `titleRes` → id。
 * 插件 provider 抛出的任何异常都被吞掉并回退，不能让宿主导航/搜索页崩溃。
 */
object PluginLabelResolver {

    fun navLabel(context: Context, entry: NavEntry): String =
        safeProvider(context, entry.titleProvider)
            ?: entry.textTitle?.takeIf { it.isNotBlank() }
            ?: stringOrNull(context, entry.titleRes)
            ?: entry.id

    fun textNavLabel(context: Context, entry: TextNavEntry): String =
        safeProvider(context, entry.titleProvider)
            ?: entry.title.ifBlank { entry.id }

    fun searchTabLabel(context: Context, tab: SearchTab): String =
        safeProvider(context, tab.titleProvider)
            ?: tab.textTitle?.takeIf { it.isNotBlank() }
            ?: stringOrNull(context, tab.titleRes)
            ?: tab.id

    fun viewSearchTabLabel(context: Context, tab: ViewSearchTab): String =
        safeProvider(context, tab.titleProvider)
            ?: tab.title.ifBlank { tab.id }

    fun tabLabel(context: Context, tab: TabContribution): String =
        safeProvider(context, tab.titleProvider)
            ?: stringOrNull(context, tab.titleRes)
            ?: tab.id

    fun composePluginLabel(context: Context, plugin: ComposeUiPlugin): String {
        // 只调用可选接口；不直接调用 ComposeUiPlugin.localizedDisplayName，
        // 因为该 default 方法在 DEX 中可能退化为 abstract（旧 APK 无 bridge）。
        val localized = if (plugin is LocalizedComposeUiPlugin) {
            try {
                plugin.localizedDisplayName(context).takeIf { it.isNotBlank() }
            } catch (t: Throwable) {
                if (t is VirtualMachineError) throw t
                null
            }
        } else {
            null
        }
        return localized ?: plugin.displayName
    }

    private fun safeProvider(
        context: Context,
        provider: ((Context) -> String)?,
    ): String? {
        if (provider == null) return null
        return try {
            provider(context).takeIf { it.isNotBlank() }
        } catch (t: Throwable) {
            if (t is VirtualMachineError) throw t
            null
        }
    }

    private fun stringOrNull(context: Context, resId: Int): String? {
        if (resId == 0) return null
        return try {
            context.getString(resId).takeIf { it.isNotBlank() }
        } catch (t: Throwable) {
            if (t is VirtualMachineError) throw t
            null
        }
    }
}
