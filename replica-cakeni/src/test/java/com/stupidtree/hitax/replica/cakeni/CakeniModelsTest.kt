package com.stupidtree.hitax.replica.cakeni

import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.TimeInDay
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.data.model.timetable.Timetable
import org.junit.Assert.assertEquals
import org.junit.Test
import java.sql.Timestamp
import java.util.Calendar

class CakeniModelsTest {
    private fun monday(): Long = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 23, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun timetable(): Timetable = Timetable().apply {
        id = "t1"
        name = "测试课表"
        startTime = Timestamp(monday())
        endTime = Timestamp(monday() + 12L * 7 * 24 * 3600 * 1000)
        scheduleStructure = (0 until 6).map {
            TimePeriodInDay(TimeInDay(8 + it, 0), TimeInDay(8 + it, 45))
        }
    }

    private fun event(name: String, dayOffset: Int, fromNumber: Int, lastNumber: Int): EventItem =
        EventItem().apply {
            id = "$name-$dayOffset-$fromNumber"
            this.name = name
            teacher = "老师"
            place = "A101"
            timetableId = "t1"
            from = Timestamp(monday() + dayOffset * 24L * 3600 * 1000 + fromNumber * 3600 * 1000)
            to = Timestamp(from.time + 45L * 60 * 1000)
            this.fromNumber = fromNumber
            this.lastNumber = lastNumber
        }

    @Test
    fun buildWeekMapsDayAndDuration() {
        val week = buildCakeniWeek(listOf(event("数学", 1, 5, 2)), timetable(), 1)
        val course = week.days[1].courses.single()
        assertEquals(1, course.dayIndex)
        assertEquals(5, course.startSlot)
        assertEquals(6, course.endSlot)
    }

    @Test
    fun fiveDayWeekOmitsWeekend() {
        val week = buildCakeniWeek(emptyList(), timetable(), 1, dayCount = 5)
        assertEquals(5, week.days.size)
    }

    @Test
    fun overlapClustersDoNotShrinkIsolatedCourse() {
        val a = CakeniCourse("a", "A", "", "", 0, 0, 0, 1, 2, 0, EventItem.TYPE.CLASS)
        val b = CakeniCourse("b", "B", "", "", 0, 0, 0, 2, 3, 0, EventItem.TYPE.CLASS)
        val c = CakeniCourse("c", "C", "", "", 0, 0, 0, 5, 6, 0, EventItem.TYPE.CLASS)
        val blocks = layoutCakeniDay(listOf(a, b, c))
        assertEquals(2, blocks.first { it.course.id == "a" }.laneCount)
        assertEquals(1, blocks.first { it.course.id == "c" }.laneCount)
    }
}
