package com.stupidtree.hitax.data.work

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/** Synthetic payload covering the 24-hour reminder schedule. */
class ExamReminderFixtureScheduleTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun payloadSchedules24HourReminder() {
        val start = ExamReminderSchedule.startMillis("2026-03-14", "19:00-21:00", zone)
        assertNotNull(start)
        val exam = ExamReminderSchedule.Exam(
            id = "fixture|2026-03-14|19:00-21:00",
            name = "示例课程",
            place = "测试教室",
            startMillis = start!!,
        )
        val now = ZonedDateTime.of(2026, 3, 1, 0, 0, 0, 0, zone).toInstant().toEpochMilli()

        val next = ExamReminderSchedule.nextDue(listOf(exam), now)

        assertNotNull(next)
        assertEquals(24 * 60, next!!.leadMinutes)
        assertEquals(
            ZonedDateTime.of(2026, 3, 13, 19, 0, 0, 0, zone).toInstant().toEpochMilli(),
            next.fireAtMillis,
        )
    }
}
