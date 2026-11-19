package com.stupidtree.hita.ui.homework

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeworkDateTest {
    @Test
    fun acceptsIsoDate() {
        assertEquals("2026-11-06", HomeworkStore.normalizeDate("2026-11-06"))
    }

    @Test
    fun rejectsInvalidDates() {
        assertNull(HomeworkStore.normalizeDate("2026-02-30"))
        assertNull(HomeworkStore.normalizeDate("2026/11/06"))
        assertNull(HomeworkStore.normalizeDate(""))
    }

    @Test
    fun trimsWhitespace() {
        assertEquals("2026-11-06", HomeworkStore.normalizeDate(" 2026-11-06 "))
    }
}
