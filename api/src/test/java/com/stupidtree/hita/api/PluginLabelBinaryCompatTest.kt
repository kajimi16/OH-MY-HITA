package com.stupidtree.hita.api

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.fragment.app.Fragment
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginLabelBinaryCompatTest {

    @Test
    fun oldNavEntryConstructorsKeepTheirDescriptors() {
        assertNotNull(
            NavEntry::class.java.getConstructor(
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Function1::class.java,
            )
        )
        assertNotNull(
            NavEntry::class.java.getConstructor(
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java,
                Drawable::class.java,
                Function1::class.java,
            )
        )

        val oldFive = NavEntry("nav", 0, 0, 1) { null }
        val oldSeven = NavEntry("nav7", 0, 0, 1, null, null) { null }
        assertNull(oldFive.titleProvider)
        assertNull(oldSeven.titleProvider)
    }

    @Test
    fun oldOtherContributionConstructorsKeepTheirDescriptors() {
        assertNotNull(
            TextNavEntry::class.java.getConstructor(
                String::class.java,
                String::class.java,
                Int::class.javaPrimitiveType,
                Drawable::class.java,
                Function1::class.java,
            )
        )
        assertNotNull(
            SearchTab::class.java.getConstructor(
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Function0::class.java,
            )
        )
        assertNotNull(
            ViewSearchTab::class.java.getConstructor(
                String::class.java,
                String::class.java,
                Int::class.javaPrimitiveType,
                Function1::class.java,
                Function1::class.java,
            )
        )
        assertNotNull(
            TabContribution::class.java.getConstructor(
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Function0::class.java,
            )
        )
    }

    @Test
    fun oldComposePluginImplementationNeedsNoLocalizedNameOverride() {
        val oldPlugin = object : ComposeUiPlugin {
            override val id = "old.plugin"
            override val displayName = "旧插件"
            override fun createView(context: Context, host: PluginUiHost): View = error("unused")
        }
        assertNotNull(oldPlugin)
        assertFalse(oldPlugin is LocalizedComposeUiPlugin)
        assertTrue(
            ComposeUiPlugin::class.java
                .getMethod("localizedDisplayName", Context::class.java)
                .isDefault
        )
    }

    @Test
    fun optionalLocalizedInterfaceIsSeparateFromOldComposeContract() {
        val newPlugin = object : ComposeUiPlugin, LocalizedComposeUiPlugin {
            override val id = "new.plugin"
            override val displayName = "旧名称"
            override fun createView(context: Context, host: PluginUiHost): View = error("unused")
            override fun localizedDisplayName(context: Context): String = "本地化名称"
        }
        assertTrue(newPlugin is LocalizedComposeUiPlugin)
        assertEquals("本地化名称", newPlugin.localizedDisplayName(mockContext()))
    }

    private fun mockContext(): Context = android.content.ContextWrapper(null)
}
