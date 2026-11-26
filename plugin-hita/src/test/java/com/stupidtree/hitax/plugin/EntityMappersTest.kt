package com.stupidtree.hitax.plugin

import com.stupidtree.hita.api.dto.DtoJson
import com.stupidtree.hita.api.dto.EventDto
import com.stupidtree.hita.api.dto.SubjectColorDto
import com.stupidtree.hita.api.dto.TeacherDto
import com.stupidtree.hita.api.dto.TimetableDefaults
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.SubjectColor
import com.stupidtree.hitax.data.model.timetable.TeacherInfo
import com.stupidtree.hitax.data.model.timetable.TermSubject
import com.stupidtree.hitax.data.model.timetable.Timetable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.Timestamp

class EntityMappersTest {

    @Test
    fun defaultPeriodsMatchTimetableModelExactly() {
        val expected = Timetable().getDefaultTimeStructure()
        val actual = TimetableDefaults.periods()

        assertEquals(12, actual.size)
        expected.zip(actual).forEach { (entity, dto) ->
            assertEquals(entity.from.hour, dto.fromHour)
            assertEquals(entity.from.minute, dto.fromMinute)
            assertEquals(entity.to.hour, dto.toHour)
            assertEquals(entity.to.minute, dto.toMinute)
        }
    }

    @Test
    fun timetablePreservesTimestampMillisAndPeriodOrder() {
        val timetable = Timetable().apply {
            id = "t1"
            name = "2026 春"
            code = "2026-1"
            providerId = "hit.shenzhen"
            startTime = Timestamp(1_744_000_000_123L)
            endTime = Timestamp(1_746_000_000_456L)
            createdAt = Timestamp(1_743_000_000_789L)
            scheduleStructure = TimetableDefaults.periods().map { it.toEntity() }
        }

        val dto = timetable.toDto()
        assertEquals(1_744_000_000_123L, dto.startTime)
        assertEquals(1_746_000_000_456L, dto.endTime)
        assertEquals(1_743_000_000_789L, dto.createdAt)
        assertEquals(12, dto.periods.size)
        assertEquals(8, dto.periods.first().fromHour)
        assertEquals(30, dto.periods.first().fromMinute)

        val back = dto.toEntity()
        assertEquals("t1", back.id)
        assertEquals("2026 春", back.name)
        assertEquals("2026-1", back.code)
        assertEquals("hit.shenzhen", back.providerId)
        assertEquals(1_744_000_000_123L, back.startTime.time)
        assertEquals(1_746_000_000_456L, back.endTime.time)
        assertEquals(1_743_000_000_789L, back.createdAt.time)
        assertEquals(12, back.scheduleStructure.size)
    }

    @Test
    fun emptyPeriodsFallBackToDefaultStructure() {
        val dto = com.stupidtree.hita.api.dto.TimetableDto(id = "t1", periods = emptyList())
        assertEquals(12, dto.toEntity().scheduleStructure.size)
    }

    @Test
    fun eventPreservesIgnoreColorAndType() {
        val event = EventItem().apply {
            id = "e1"
            type = EventItem.TYPE.EXAM
            name = "高等数学"
            place = "A101"
            teacher = "张老师"
            subjectId = "s1"
            timetableId = "t1"
            from = Timestamp(1_744_000_000_123L)
            to = Timestamp(1_744_000_600_456L)
            fromNumber = 1
            lastNumber = 2
            createdAt = Timestamp(1_743_000_000_789L)
            color = 0xFF112233.toInt()
        }

        val dto = event.toDto()
        assertEquals("EXAM", dto.type)
        assertEquals(0xFF112233.toInt(), dto.color)
        assertEquals(1_744_000_000_123L, dto.from)
        assertEquals(1_744_000_600_456L, dto.to)
        assertEquals(1_743_000_000_789L, dto.createdAt)

        val back = dto.toEntity()
        assertEquals("e1", back.id)
        assertEquals(EventItem.TYPE.EXAM, back.type)
        assertEquals("A101", back.place)
        assertEquals("张老师", back.teacher)
        assertEquals(0xFF112233.toInt(), back.color)
        assertEquals(1_744_000_000_123L, back.from.time)
        assertEquals(1_744_000_600_456L, back.to.time)
        assertEquals(1_743_000_000_789L, back.createdAt.time)
    }

    @Test
    fun unknownEventTypeFallsBackToOther() {
        val dto = EventDto(id = "e1", type = "NOT_A_TYPE", name = "x")
        assertEquals(EventItem.TYPE.OTHER, dto.toEntity().type)
    }

    @Test
    fun subjectPreservesIgnoreFieldsAndCountInSpa() {
        val subject = TermSubject().apply {
            id = "s1"
            name = "高等数学"
            timetableId = "t1"
            type = TermSubject.TYPE.OPT_A
            field = "自然科学"
            selectCategory = "专选"
            nature = "选修"
            credit = 3.5f
            school = "计算机学院"
            countInSPA = true
            code = "MATH101"
            key = "math-key"
            teacher = "张老师"
            externalCourseId = "ext-1"
            taskNumber = "task-1"
            selectionRequestId = "sel-1"
            createdAt = Timestamp(1_743_000_000_789L)
            color = 1001
        }

        val dto = subject.toDto()
        assertTrue(dto.countInSpa)
        assertEquals("OPT_A", dto.type)
        assertEquals("张老师", dto.teacher)
        assertEquals("ext-1", dto.externalCourseId)
        assertEquals("task-1", dto.taskNumber)
        assertEquals("sel-1", dto.selectionRequestId)

        val json = DtoJson.toJson(dto)
        assertTrue(json.contains("\"countInSPA\":true"))
        assertFalse(json.contains("\"countInSpa\""))

        val back = dto.toEntity()
        assertEquals("s1", back.id)
        assertEquals(TermSubject.TYPE.OPT_A, back.type)
        assertTrue(back.countInSPA)
        assertEquals("张老师", back.teacher)
        assertEquals("ext-1", back.externalCourseId)
        assertEquals("task-1", back.taskNumber)
        assertEquals("sel-1", back.selectionRequestId)
        assertEquals(1_743_000_000_789L, back.createdAt.time)
        assertEquals(1001, back.color)
    }

    @Test
    fun teacherAndSubjectColorRoundTrip() {
        val teacher = TeacherInfo().apply {
            name = "张老师"
            subjectName = "高等数学"
        }
        assertEquals(teacher, teacher.toDto().toEntity())

        val color = SubjectColor().apply {
            id = "s1"
            color = 1001
        }
        val colorBack = color.toDto().toEntity()
        assertEquals("s1", colorBack.id)
        assertEquals(1001, colorBack.color)
        assertEquals(TeacherDto(name = "张老师", subjectName = "高等数学"), teacher.toDto())
        assertEquals(SubjectColorDto(id = "s1", color = 1001), color.toDto())
    }
}
