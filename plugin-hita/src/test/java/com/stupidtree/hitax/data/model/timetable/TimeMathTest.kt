package com.stupidtree.hitax.data.model.timetable

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TimeMathTest {

    @Test
    fun getMondayReturnsMondayMidnight() {
        val anyTimestamp = 1_790_000_000_000L // 2026-09 附近
        val monday = TimeMath.getMonday(anyTimestamp)

        assertEquals(Calendar.MONDAY, monday.get(Calendar.DAY_OF_WEEK))
        assertEquals(0, monday.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, monday.get(Calendar.MINUTE))
        // 落在同一周内，且不晚于原时间
        assertEquals(true, monday.timeInMillis <= anyTimestamp)
        assertEquals(true, anyTimestamp - monday.timeInMillis < WEEK_MILLS)
    }

    @Test
    fun getDowTreatsMondayAsOne() {
        val monday = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 5, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(1, TimeMath.getDow(monday.timeInMillis))

        val sunday = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 11, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(7, TimeMath.getDow(sunday.timeInMillis))
    }

    @Test
    fun hourAndMinuteUseGmtPlusEight() {
        // 时间戳 0 = UTC 1970-01-01 00:00，按东八区应为 08:00
        assertEquals(8, TimeMath.getHour(0L))
        assertEquals(0, TimeMath.getMinute(0L))
        // 90 秒 = 1 分 30 秒
        assertEquals(1, TimeMath.getMinute(90_000L))
        // 东八区 23:00 对应 UTC 15:00
        assertEquals(23, TimeMath.getHour(15 * 60 * 60 * 1000L))
    }
}
