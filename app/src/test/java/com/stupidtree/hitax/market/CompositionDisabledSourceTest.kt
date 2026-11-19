package com.stupidtree.hitax.market

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositionDisabledSourceTest {
    @Test
    fun presetDisableIsNotTreatedAsRequiredUserDisable() {
        val oldAndroidEntries = listOf(
            CompositionEntry(
                id = "com.stupidtree.hita.ui.homework",
                kind = CompositionKind.UI,
                enabled = false,
                required = true,
                source = CompositionSource.PRESET,
            ),
            CompositionEntry(
                id = "com.stupidtree.hitax.timetable",
                kind = CompositionKind.BUILTIN,
                enabled = true,
                required = true,
                source = CompositionSource.PRESET,
            ),
        )
        val plan = CompositionPlan(entries = oldAndroidEntries, missing = emptyList())

        assertEquals(
            setOf(CompositionTarget(CompositionKind.UI, "com.stupidtree.hita.ui.homework")),
            plan.automaticDisabledTargets(),
        )
        assertTrue(plan.entries.none { it.enabled && it.source == CompositionSource.USER })
    }

    @Test
    fun userDisableKeepsPrecedenceOverAutomaticDisable() {
        val entries = listOf(
            CompositionEntry(
                id = "com.stupidtree.hita.ui.ai",
                kind = CompositionKind.UI,
                enabled = false,
                required = true,
                source = CompositionSource.USER,
            ),
            CompositionEntry(
                id = "com.stupidtree.hita.ui.news",
                kind = CompositionKind.UI,
                enabled = false,
                required = true,
                source = CompositionSource.PRESET,
            ),
        )
        val plan = CompositionPlan(entries = entries, missing = emptyList())

        assertEquals(
            setOf(CompositionTarget(CompositionKind.UI, "com.stupidtree.hita.ui.news")),
            plan.automaticDisabledTargets(),
        )
    }
}
