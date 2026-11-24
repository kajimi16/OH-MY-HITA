package com.stupidtree.hita.plugin.todo.geniusta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime

class GeniusTaParserSectionTest {

    private val course = GeniusTaCourse("c-1", "Math", "sec-1")

    @Test
    fun usesTheMatchingSectionSchedule() {
        val payload = "{\"data\":[{\"public_id\":\"a-1\",\"title\":\"HW\"," +
            "\"is_released\":true,\"submission_state\":{\"status\":\"not_submitted\"}," +
            "\"section_schedules\":[" +
            "{\"section_id\":\"sec-2\",\"due_date\":\"2026-03-05T10:00:00+08:00\"}," +
            "{\"section_id\":\"sec-1\",\"due_date\":\"2026-03-04T09:00:00+08:00\"}]}]}"

        val assignment = GeniusTaParser.assignments(payload, course).single()

        assertEquals(
            OffsetDateTime.parse("2026-03-04T09:00:00+08:00").toInstant().toEpochMilli(),
            assignment.dueAt,
        )
    }

    @Test
    fun explicitNullSectionDeadlineCancelsTheBaseDeadline() {
        val payload = "{\"data\":[{\"public_id\":\"a-1\",\"title\":\"HW\"," +
            "\"is_released\":true,\"submission_state\":{\"status\":\"not_submitted\"}," +
            "\"section_schedules\":[{\"section_id\":\"sec-1\",\"due_date\":null}]}]}"

        assertTrue(GeniusTaParser.assignments(payload, course).isEmpty())
    }

    @Test
    fun rejectsDuplicateMatchesAndUnknownSections() {
        val duplicate = "{\"data\":[{\"public_id\":\"a-1\",\"title\":\"HW\"," +
            "\"is_released\":true,\"submission_state\":{\"status\":\"not_submitted\"}," +
            "\"section_schedules\":[" +
            "{\"section_id\":\"sec-1\",\"due_date\":null}," +
            "{\"section_id\":\"sec-1\",\"due_date\":null}]}]}"
        val unknown = "{\"data\":[{\"public_id\":\"a-1\",\"title\":\"HW\"," +
            "\"is_released\":true,\"submission_state\":{\"status\":\"not_submitted\"}," +
            "\"section_schedules\":[{\"section_id\":\"sec-2\",\"due_date\":null}]}]}"

        assertThrows(GeniusTaInvalidResponseException::class.java) {
            GeniusTaParser.assignments(duplicate, course)
        }
        assertThrows(GeniusTaInvalidResponseException::class.java) {
            GeniusTaParser.assignments(unknown, course)
        }
    }
}
