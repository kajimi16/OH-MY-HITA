package com.stupidtree.hitax.calendar

import com.stupidtree.hitax.data.model.timetable.EventItem
import java.sql.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarSyncPlannerTest {

    private fun event(
        id: String,
        timetableId: String = "t1",
        type: EventItem.TYPE = EventItem.TYPE.CLASS,
        from: Long = 1_000,
        to: Long = 2_000,
    ) = EventItem().apply {
        this.id = id
        this.timetableId = timetableId
        this.type = type
        this.name = "课程-$id"
        this.place = "A101"
        this.from = Timestamp(from)
        this.to = Timestamp(to)
    }

    @Test
    fun entriesOnlyContainClassEventsOfSelectedTimetable() {
        val entries = CalendarSyncPlanner.entries(
            events = listOf(
                event("a"),
                event("b", timetableId = "t2"),
                event("c", type = EventItem.TYPE.EXAM),
                event("d", from = 10_000, to = 5_000),
            ),
            timetableId = "t1",
            timetableName = "主课表",
        )
        assertEquals(listOf("hita:t1:a"), entries.map { it.key })
        assertTrue(entries.single().description.contains("主课表"))
    }

    @Test
    fun keyFromDescriptionFindsKeyAfterHumanDescription() {
        assertEquals(
            "hita:t1:a",
            CalendarSyncPlanner.keyFromDescription("主课表\nhita:t1:a"),
        )
        assertEquals(null, CalendarSyncPlanner.keyFromDescription("主课表"))
    }

    @Test
    fun planSeparatesInsertUpdateAndDelete() {
        val entries = listOf(
            CalendarSyncPlanner.entries(listOf(event("a"), event("b")), "t1", null).first(),
            CalendarSyncPlanner.entries(listOf(event("b")), "t1", null).first(),
        )
        val plan = CalendarSyncPlanner.plan(entries, setOf("hita:t1:b", "hita:t1:old"))
        assertEquals(listOf("hita:t1:a"), plan.inserts.map { it.key })
        assertEquals(listOf("hita:t1:b"), plan.updates.map { it.key })
        assertEquals(listOf("hita:t1:old"), plan.deletes)
    }
}
