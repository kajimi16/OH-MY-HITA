package com.stupidtree.hita.ui.compare

import com.stupidtree.hitax.data.model.timetable.EventItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.sql.Timestamp

class CompareLogicTest {
    private fun event(name: String, from: Long, to: Long) = EventItem().apply {
        this.name = name
        this.from = Timestamp(from)
        this.to = Timestamp(to)
        type = EventItem.TYPE.CLASS
    }

    @Test
    fun overlappingEventsAreDetected() {
        val a = listOf(event("高数", 8_000, 10_000))
        val b = listOf(event("大物", 9_000, 11_000))
        assertEquals(1, CompareLogic.overlaps(a, b).size)
    }

    @Test
    fun adjacentEventsAreNotOverlapping() {
        val a = listOf(event("高数", 8_000, 9_000))
        val b = listOf(event("大物", 9_000, 10_000))
        assertEquals(0, CompareLogic.overlaps(a, b).size)
    }
}
