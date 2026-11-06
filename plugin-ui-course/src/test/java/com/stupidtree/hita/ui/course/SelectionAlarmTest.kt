package com.stupidtree.hita.ui.course

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class SelectionAlarmTest {
    @Test
    fun futureTimeToday() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 6, 8, 0, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val trigger = SelectionAlarm.nextTrigger("09:30", now)!!
        val c = Calendar.getInstance().apply { timeInMillis = trigger }
        assertEquals(9, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, c.get(Calendar.MINUTE))
        assertEquals(6, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun pastTimeRollsToTomorrow() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 6, 10, 0, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val trigger = SelectionAlarm.nextTrigger("09:30", now)!!
        assertEquals(7, Calendar.getInstance().apply { timeInMillis = trigger }.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun invalidInputRejected() {
        assertNull(SelectionAlarm.nextTrigger("25:00", 0))
        assertNull(SelectionAlarm.nextTrigger("08:60", 0))
        assertNull(SelectionAlarm.nextTrigger("bad", 0))
    }
}
