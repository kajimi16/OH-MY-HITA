package com.stupidtree.hitax.shell

import com.stupidtree.hitax.data.model.timetable.EventItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ShellTimetableChangeTest {

    private fun event(
        id: String = "e1",
        name: String = "计算机网络",
        start: Long = 1_700_000_000_000,
        end: Long = 1_700_003_600_000,
    ) = EventItem().apply {
        this.id = id
        this.name = name
        this.subjectId = "s1"
        this.from.time = start
        this.to.time = end
        this.place = "H403"
    }

    @Test
    fun `identical courses produce no change`() {
        val snapshot = ShellTimetableChange.snapshot(listOf(event()))
        assertEquals(0, ShellTimetableChange.diff(snapshot, snapshot))
    }

    @Test
    fun `added course counts once`() {
        val previous = ShellTimetableChange.snapshot(listOf(event(id = "e1")))
        val current = ShellTimetableChange.snapshot(listOf(event(id = "e1"), event(id = "e2")))
        assertEquals(1, ShellTimetableChange.diff(previous, current))
    }

    @Test
    fun `removed course counts once`() {
        val previous = ShellTimetableChange.snapshot(listOf(event(id = "e1"), event(id = "e2")))
        val current = ShellTimetableChange.snapshot(listOf(event(id = "e1")))
        assertEquals(1, ShellTimetableChange.diff(previous, current))
    }

    @Test
    fun `time or place change counts once per event`() {
        val previous = ShellTimetableChange.snapshot(listOf(event()))
        val current = ShellTimetableChange.snapshot(listOf(event(name = "计算机网络（调整）")))
        assertEquals(1, ShellTimetableChange.diff(previous, current))
    }

    @Test
    fun `color only changes do not count`() {
        val previous = ShellTimetableChange.snapshot(listOf(event()))
        val recolored = event().apply { color = 0xFFFF0000.toInt() }
        assertEquals(0, ShellTimetableChange.diff(previous, ShellTimetableChange.snapshot(listOf(recolored))))
    }
}
