package com.stupidtree.hitax.data.repository

import com.stupidtree.hitax.data.model.timetable.EventItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.sql.Timestamp

class TimetableEventDedupeTest {
    @Test
    fun apply_mergesIdenticalClassEventsFromDifferentTimetables() {
        val first = event("操作系统", "H313", "张老师", timetableId = "eas", subjectId = "subject-a")
        val second = event("操作系统", "H313", "张老师", timetableId = "default", subjectId = "subject-b")

        val result = TimetableEventDedupe.apply(listOf(first, second))

        assertEquals(listOf(first), result)
    }

    @Test
    fun apply_keepsDifferentCoursesAtTheSameTime() {
        val operatingSystem = event("操作系统", "H313", "张老师")
        val computerNetwork = event("计算机网络", "H403", "李老师")

        val result = TimetableEventDedupe.apply(listOf(operatingSystem, computerNetwork))

        assertEquals(listOf(operatingSystem, computerNetwork), result)
    }

    @Test
    fun apply_keepsSameCourseAtDifferentTimes() {
        val first = event("操作系统", "H313", "张老师", from = 1_000_000L)
        val second = event("操作系统", "H313", "张老师", from = 2_000_000L, to = 2_360_000L)

        val result = TimetableEventDedupe.apply(listOf(first, second))

        assertEquals(listOf(first, second), result)
    }

    @Test
    fun apply_doesNotMergeNonClassEvents() {
        val first = event("自习", "图书馆", "").apply { type = EventItem.TYPE.OTHER }
        val second = event("自习", "图书馆", "").apply { type = EventItem.TYPE.OTHER }

        val result = TimetableEventDedupe.apply(listOf(first, second))

        assertEquals(listOf(first, second), result)
    }

    @Test
    fun apply_keepsTheFirstOccurrenceOrder() {
        val first = event("操作系统", "H313", "张老师")
        val second = event("计算机网络", "H403", "李老师")
        val duplicate = event("操作系统", "H313", "张老师")

        val result = TimetableEventDedupe.apply(listOf(first, second, duplicate))

        assertEquals(listOf(first, second), result)
    }

    private fun event(
        name: String,
        place: String,
        teacher: String,
        from: Long = 1_000_000L,
        to: Long = 1_360_000L,
        timetableId: String = "timetable",
        subjectId: String = "subject",
    ): EventItem = EventItem().apply {
        this.name = name
        this.place = place
        this.teacher = teacher
        this.from = Timestamp(from)
        this.to = Timestamp(to)
        this.fromNumber = 1
        this.lastNumber = 2
        this.timetableId = timetableId
        this.subjectId = subjectId
    }
}
