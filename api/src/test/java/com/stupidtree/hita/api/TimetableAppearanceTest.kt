package com.stupidtree.hita.api

import org.junit.Assert.*
import org.junit.Test

class TimetableAppearanceTest {
    @Test fun hostileSettingsAreBoundedBeforeRendering() {
        val value = TimetableAppearance(Int.MIN_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, true, 7).normalized()
        assertEquals(0, value.cardOpacity)
        assertEquals(30, value.blurRadius)
        assertEquals(80, value.scrimOpacity)
        assertTrue(value.hasWallpaper)
        assertEquals(7, value.revision)
    }
    @Test fun validZeroOpacityAndNoBlurRemainExpressible() {
        val value = TimetableAppearance(0, 0, 0).normalized()
        assertEquals(TimetableAppearance(0, 0, 0), value)
        assertEquals(TimetableAppearance(100, 0, 0), TimetableAppearance(200, -5, -8).normalized())
    }
}
