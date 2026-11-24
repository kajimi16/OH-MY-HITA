package com.stupidtree.hitax.shell

import com.stupidtree.hitax.data.model.timetable.EventItem
import java.sql.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TimetableHistoryDiffTest {

    private fun event(
        id: String,
        timetableId: String = "t1",
        type: EventItem.TYPE = EventItem.TYPE.CLASS,
        name: String = "课程",
        from: Long = 1_000,
        to: Long = 2_000,
        place: String? = "A101",
    ) = EventItem().apply {
        this.id = id
        this.timetableId = timetableId
        this.type = type
        this.name = name
        this.from = Timestamp(from)
        this.to = Timestamp(to)
        this.place = place
    }

    @Test
    fun eventsForKeepsOnlyClassEventsOfSelectedTimetable() {
        val events = listOf(
            event("a", timetableId = "t1"),
            event("b", timetableId = "t2"),
            event("c", timetableId = "t1", type = EventItem.TYPE.EXAM),
        )
        assertEquals(listOf("a"), TimetableHistoryDiff.eventsFor("t1", events).map { it.id })
    }

    @Test
    fun fingerprintIgnoresOtherEventTypesAndDetectsClassChanges() {
        val base = listOf(event("a"))
        val same = listOf(event("a"), event("exam", type = EventItem.TYPE.EXAM))
        val moved = listOf(event("a", from = 5_000, to = 6_000))

        assertEquals(
            TimetableHistoryDiff.fingerprint(base),
            TimetableHistoryDiff.fingerprint(same),
        )
        assertNotEquals(
            TimetableHistoryDiff.fingerprint(base),
            TimetableHistoryDiff.fingerprint(moved),
        )
    }

    @Test
    fun diffClassifiesAddedRemovedMovedAndDetails() {
        val before = listOf(
            event("removed"),
            event("moved", from = 1_000, to = 2_000),
            event("details", name = "旧名"),
        )
        val after = listOf(
            event("moved", from = 3_000, to = 4_000),
            event("details", name = "新名"),
            event("added"),
        )

        val diff = TimetableHistoryDiff.diff(before, after)

        assertEquals(1, diff.count(TimetableChangeKind.REMOVED))
        assertEquals(1, diff.count(TimetableChangeKind.ADDED))
        assertEquals(1, diff.count(TimetableChangeKind.MOVED))
        assertEquals(1, diff.count(TimetableChangeKind.DETAILS))
    }
}
