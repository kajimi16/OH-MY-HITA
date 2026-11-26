package com.stupidtree.hitax.plugin

import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.fragment.app.Fragment
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.LocalizedComposeUiPlugin
import com.stupidtree.hita.api.NavEntry
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.SearchTab
import com.stupidtree.hita.api.TabContribution
import com.stupidtree.hita.api.TextNavEntry
import com.stupidtree.hita.api.ViewSearchTab
import org.junit.Assert.assertEquals
import org.junit.Test

class PluginLabelResolverTest {

    private val context: Context = ContextWrapper(null)

    @Test
    fun navProviderWinsOverFallbacks() {
        val entry = NavEntry("nav", 0, 0, 1, "fallback", null, { null }, { "本地化" })
        assertEquals("本地化", PluginLabelResolver.navLabel(context, entry))
    }

    @Test
    fun navProviderExceptionFallsBackToTextTitle() {
        val entry = NavEntry("nav", 0, 0, 1, "fallback", null, { null }, { error("boom") })
        assertEquals("fallback", PluginLabelResolver.navLabel(context, entry))
    }

    @Test
    fun blankProviderFallsBack() {
        val entry = NavEntry("nav", 0, 0, 1, "fallback", null, { null }, { "   " })
        assertEquals("fallback", PluginLabelResolver.navLabel(context, entry))
    }

    @Test
    fun oldConstructorsStillResolve() {
        assertEquals(
            "nav",
            PluginLabelResolver.navLabel(context, NavEntry("nav", 0, 0, 1) { null }),
        )
        assertEquals(
            "Text",
            PluginLabelResolver.textNavLabel(
                context,
                TextNavEntry("text-nav", "Text", 1, null) { null },
            ),
        )
        assertEquals(
            "search",
            PluginLabelResolver.searchTabLabel(
                context,
                SearchTab("search", 0, 1) { error("unused") },
            ),
        )
        assertEquals(
            "View",
            PluginLabelResolver.viewSearchTabLabel(
                context,
                ViewSearchTab("view-search", "View", 1, { error("unused") }),
            ),
        )
        assertEquals(
            "tab",
            PluginLabelResolver.tabLabel(
                context,
                TabContribution("tab", 0, 0, 1) { error("unused") },
            ),
        )
    }

    @Test
    fun oldComposePluginFallsBackToDisplayName() {
        val plugin = object : ComposeUiPlugin {
            override val id = "old.plugin"
            override val displayName = "旧插件"
            override fun createView(context: Context, host: PluginUiHost): View = error("unused")
        }
        assertEquals("旧插件", PluginLabelResolver.composePluginLabel(context, plugin))
    }

    @Test
    fun oldPluginOverridingComposeDefaultWithoutMarkerIsIgnored() {
        // 旧 APK 即使因其他原因有同名方法，只要没实现可选接口，宿主就不调用它。
        val plugin = object : ComposeUiPlugin {
            override val id = "old.plugin"
            override val displayName = "旧插件"
            override fun createView(context: Context, host: PluginUiHost): View = error("unused")
            override fun localizedDisplayName(context: Context): String = "不应被调用"
        }
        assertEquals("旧插件", PluginLabelResolver.composePluginLabel(context, plugin))
    }

    @Test
    fun markedLocalizedPluginUsesOptionalInterface() {
        val plugin = object : ComposeUiPlugin, LocalizedComposeUiPlugin {
            override val id = "new.plugin"
            override val displayName = "旧名称"
            override fun createView(context: Context, host: PluginUiHost): View = error("unused")
            override fun localizedDisplayName(context: Context): String = "本地化名称"
        }
        assertEquals("本地化名称", PluginLabelResolver.composePluginLabel(context, plugin))
    }
}
