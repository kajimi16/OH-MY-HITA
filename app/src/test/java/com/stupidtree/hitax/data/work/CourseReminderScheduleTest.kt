package com.stupidtree.hitax.data.work

import com.stupidtree.hitax.data.model.timetable.EventItem
import java.sql.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseReminderScheduleTest {

    private val now = 1_000_000_000L

    private fun event(
        id: String,
        fromOffsetMs: Long,
        toOffsetMs: Long,
        type: EventItem.TYPE = EventItem.TYPE.CLASS,
    ) = EventItem().apply {
        this.id = id
        this.type = type
        this.from = Timestamp(now + fromOffsetMs)
        this.to = Timestamp(now + toOffsetMs)
    }

    @Test
    fun startDueFindsUpcomingAndHonorsLegacyAndPhaseKeys() {
        val events = listOf(
            event("a", 5 * 60_000, 60 * 60_000),
            event("b", 20 * 60_000, 60 * 60_000),
            event("c", 5 * 60_000, 60 * 60_000),
        )
        val due = CourseReminderSchedule.startDue(
            events,
            now,
            leadMinutes = 10,
            sent = setOf("c", CourseReminderSchedule.START_PREFIX + "d"),
        )
        assertEquals(listOf("a"), due.map { it.id })
    }

    @Test
    fun endDueOnlyUsesClassEventsInsideLeadWindow() {
        val events = listOf(
            event("endsIn5", -60 * 60_000, 5 * 60_000),
            event("endsIn30", -60 * 60_000, 30 * 60_000),
            event("other", -60 * 60_000, 5 * 60_000, type = EventItem.TYPE.OTHER),
            event("notStarted", 10 * 60_000, 15 * 60_000),
        )
        val due = CourseReminderSchedule.endDue(
            events = events,
            now = now,
            leadMinutes = 5,
            windowEnd = now + 15 * 60_000,
            sent = emptySet(),
        )
        assertEquals(listOf("endsIn5"), due.map { it.id })
    }

    @Test
    fun endDueSkipsAlreadySentEndPhase() {
        val due = CourseReminderSchedule.endDue(
            events = listOf(event("a", -60 * 60_000, 5 * 60_000)),
            now = now,
            leadMinutes = 5,
            windowEnd = now + 15 * 60_000,
            sent = setOf(CourseReminderSchedule.END_PREFIX + "a"),
        )
        assertTrue(due.isEmpty())
    }

    @Test
    fun startAndEndKeysDoNotCollide() {
        val event = event("a", 0, 5 * 60_000)
        assertEquals("start:a", CourseReminderSchedule.startKeys(listOf(event)).single())
        assertEquals("end:a", CourseReminderSchedule.endKeys(listOf(event)).single())
    }
}
