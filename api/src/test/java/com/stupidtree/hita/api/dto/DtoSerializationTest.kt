package com.stupidtree.hita.api.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoSerializationTest {

    @Test
    fun accountRoundTripsAndSerializesNulls() {
        val dto = EasAccountDto(
            loggedIn = true,
            hasApiToken = true,
            displayName = "张三",
            subtitle = "20230001",
            studentId = "20230001",
            avatarUrl = "https://example.test/a.png",
            studentType = "UNDERGRAD",
            school = null,
            major = "软件工程",
            grade = "2023",
        )
        val json = DtoJson.toJson(dto)
        assertTrue(json.contains("\"avatarUrl\""))
        assertTrue(json.contains("\"school\":null"))
        val back = DtoJson.fromJson(json, EasAccountDto::class.java)
        assertEquals(dto, back)
    }

    @Test
    fun timetableAndPeriodsRoundTrip() {
        val dto = TimetableDto(
            id = "t1",
            name = "2026 春",
            code = "2026-1",
            providerId = "hit.shenzhen",
            startTime = 1_744_000_000_123L,
            endTime = 1_746_000_000_456L,
            createdAt = 1_743_000_000_789L,
            periods = TimetableDefaults.periods(),
        )
        val back = DtoJson.fromJson(DtoJson.toJson(dto), TimetableDto::class.java)
        assertEquals(dto, back)
        assertEquals("hit.shenzhen", back.providerId)
        assertEquals(12, back.periods.size)
    }

    @Test
    fun eventPreservesColorAndMillis() {
        val dto = EventDto(
            id = "e1",
            type = "EXAM",
            name = "高等数学",
            place = "A101",
            teacher = "张老师",
            subjectId = "s1",
            timetableId = "t1",
            from = 1_744_000_000_123L,
            to = 1_744_000_600_456L,
            fromNumber = 1,
            lastNumber = 2,
            createdAt = 1_743_000_000_789L,
            color = 0xFF112233.toInt(),
        )
        val back = DtoJson.fromJson(DtoJson.toJson(dto), EventDto::class.java)
        assertEquals(dto, back)
        assertEquals(0xFF112233.toInt(), back.color)
    }

    @Test
    fun subjectUsesCountInSpaWireName() {
        val dto = SubjectDto(
            id = "s1",
            name = "高等数学",
            timetableId = "t1",
            type = "COM_A",
            credit = 3.5f,
            countInSpa = true,
            teacher = "张老师",
            externalCourseId = "ext-1",
            taskNumber = "task-1",
            selectionRequestId = "sel-1",
            color = 1001,
        )
        val json = DtoJson.toJson(dto)
        assertTrue(json.contains("\"countInSPA\":true"))
        assertFalse(json.contains("\"countInSpa\""))
        val back = DtoJson.fromJson(json, SubjectDto::class.java)
        assertEquals(dto, back)
        assertEquals("张老师", back.teacher)
        assertEquals("ext-1", back.externalCourseId)
        assertEquals("task-1", back.taskNumber)
        assertEquals("sel-1", back.selectionRequestId)
    }

    @Test
    fun icsRequestRoundTripsBytes() {
        val dto = IcsImportRequestDto(
            fileName = "2026.ics",
            bytes = "BEGIN:VCALENDAR".toByteArray(),
        )
        val back = DtoJson.fromJson(DtoJson.toJson(dto), IcsImportRequestDto::class.java)
        assertEquals(dto, back)
        assertTrue(dto.bytes.contentEquals(back.bytes))
    }

    @Test
    fun snapshotAndChangeRoundTrip() {
        val snapshot = TimetableSnapshotDto(
            timetables = listOf(TimetableDto(id = "t1", periods = TimetableDefaults.periods())),
        )
        assertEquals(snapshot, DtoJson.fromJson(DtoJson.toJson(snapshot), TimetableSnapshotDto::class.java))

        val change = TimetableChangeDto(
            kind = "event",
            timetableIds = listOf("t1"),
            eventIds = listOf("e1", "e2"),
            subjectIds = listOf("s1"),
        )
        assertEquals(change, DtoJson.fromJson(DtoJson.toJson(change), TimetableChangeDto::class.java))
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val json = """{"schemaVersion":1,"loggedIn":false,"unknownField":"ignored"}"""
        val dto = DtoJson.fromJson(json, EasAccountDto::class.java)
        assertFalse(dto.loggedIn)
    }

    @Test
    fun accountJsonDoesNotContainCredentialKeys() {
        val json = DtoJson.toJson(EasAccountDto(loggedIn = true, displayName = "张三"))
        listOf("accessToken", "refreshToken", "password", "cookies", "webCookies")
            .forEach { secret -> assertFalse(json.contains(secret)) }
    }
}
