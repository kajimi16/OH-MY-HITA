package com.stupidtree.hitax.plugin

import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import org.junit.Assert.assertEquals
import org.junit.Test

class PluginCompositionTest {

    private fun plugin(id: String) = object : Plugin {
        override val id: String = id
        override fun install(ctx: PluginContext) = Unit
    }

    private val all = listOf(plugin("a"), plugin("b"), plugin("c"))

    @Test
    fun nullMeansAll() {
        assertEquals(listOf("a", "b", "c"), PluginComposition.select(all, null).map { it.id })
    }

    @Test
    fun emptyMeansAll() {
        assertEquals(listOf("a", "b", "c"), PluginComposition.select(all, emptySet()).map { it.id })
    }

    @Test
    fun keepsOnlyEnabledInOriginalOrder() {
        assertEquals(listOf("b", "c"), PluginComposition.select(all, setOf("c", "b")).map { it.id })
    }

    @Test
    fun unknownIdsAreIgnored() {
        assertEquals(listOf("a"), PluginComposition.select(all, setOf("a", "does.not.exist")).map { it.id })
    }
}
