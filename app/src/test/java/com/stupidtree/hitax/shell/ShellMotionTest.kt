package com.stupidtree.hitax.shell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellMotionTest {

    @Test
    fun drawer_identityAtClosed_andScaleAtOpen() {
        assertEquals(1f, ShellMotion.drawerScale("classic", 0f), 0.0001f)
        assertEquals(0.8f, ShellMotion.drawerScale("classic", 1f), 0.0001f)
        assertEquals(0.992f, ShellMotion.drawerScale("apple_glass", 1f), 0.0001f)
    }

    @Test
    fun drawer_glassOffset_usesDp_andNormalUsesWidth() {
        assertEquals(0f, ShellMotion.drawerOffsetPx("apple_glass", 0f, 800f, 2f), 0.0001f)
        assertEquals(-20f, ShellMotion.drawerOffsetPx("apple_glass", 1f, 800f, 2f), 0.0001f)
        assertEquals(-800f, ShellMotion.drawerOffsetPx("classic", 1f, 800f, 2f), 0.0001f)
        assertEquals(0f, ShellMotion.drawerOffsetPx("classic", 1f, 0f, 2f), 0.0001f)
    }

    @Test
    fun drawerDurations_matchV312() {
        assertEquals(240, ShellMotion.drawerDurationMs("apple_glass", true))
        assertEquals(200, ShellMotion.drawerDurationMs("apple_glass", false))
        assertEquals(0, ShellMotion.drawerDurationMs("classic", true))
    }

    @Test
    fun rubberBand_isBounded_andMonotonic() {
        var previous = 0f
        for (x in listOf(0.1f, 0.2f, 0.5f, 1f, 2f, 10f)) {
            val v = ShellMotion.rubberBand(x, 0.35f)
            assertTrue(v > previous)
            assertTrue(v < 0.35f)
            previous = v
        }
        assertEquals(0f, ShellMotion.rubberBand(0f, 0.35f), 0f)
    }

    @Test
    fun dockPosition_isClampedAndRubberBandedOutside() {
        val width = 400f
        val count = 6
        val padding = 4f
        assertTrue(ShellMotion.dockPositionAt(padding, width, count, padding) < 0f)
        val last = ShellMotion.dockPositionAt(width - padding, width, count, padding)
        assertTrue(last > 5f && last < 5f + ShellMotion.DOCK_OVERDRAG)
        val left = ShellMotion.dockPositionAt(0f, width, count, padding)
        assertTrue(left < 0f && left > -ShellMotion.DOCK_OVERDRAG)
        val right = ShellMotion.dockPositionAt(width, width, count, padding)
        assertTrue(right > 5f && right < 5f + ShellMotion.DOCK_OVERDRAG)
        assertEquals(0, ShellMotion.dockIndexAt(0f, width, count, padding))
        assertEquals(5, ShellMotion.dockIndexAt(width, width, count, padding))
    }

    @Test
    fun flingIndex_isProjected_andLimitedToOneItem() {
        assertEquals(1, ShellMotion.flingIndex(1f, 0f, 60f, 1, 5))
        assertEquals(2, ShellMotion.flingIndex(1f, 10_000f, 60f, 1, 5))
        assertEquals(0, ShellMotion.flingIndex(1f, -10_000f, 60f, 1, 5))
        assertEquals(4, ShellMotion.flingIndex(4f, 0f, 0f, 4, 5))
    }

    @Test
    fun pressScale_isBounded() {
        assertEquals(1f, ShellMotion.pressScale(0f, 400f, 16f), 0.0001f)
        assertEquals(1.25f, ShellMotion.pressScale(1f, 64f, 16f), 0.0001f)
        assertEquals(1.04f, ShellMotion.pressScale(1f, 400f, 16f), 0.0001f)
    }
}
