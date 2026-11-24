package com.stupidtree.hita.plugin.todo.geniusta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.OffsetDateTime

class GeniusTaParserAssignmentsTest {

    private val course = GeniusTaCourse("c-1", "Math", null)

    @Test
    fun keepsReleasedUnsubmittedRowsWithDeadlineOnly() {
        val payload = "{\"data\":[" +
            row("a-1", "HW1", true, "\"not_submitted\"", "\"2026-03-02T10:00:00+08:00\"") + "," +
            row("a-2", "HW2", false, "\"not_submitted\"", "\"2026-03-02T10:00:00+08:00\"") + "," +
            row("a-3", "HW3", true, "\"submitted\"", "\"2026-03-02T10:00:00+08:00\"") + "," +
            row("a-4", "HW4", true, "\"late_unsubmitted\"", "null") + "," +
            "{\"public_id\":\"a-5\",\"title\":\"HW5\",\"is_released\":true,\"submission_state\":null}," +
            row("a-6", "HW6", true, "\"makeup_unsubmitted\"", "\"2026-03-03T10:00:00+08:00\"") +
            "]}"

        val assignments = GeniusTaParser.assignments(payload, course)

        assertEquals(listOf("a-1", "a-6"), assignments.map { it.id })
        assertEquals("Math", assignments.first().courseTitle)
        assertEquals(
            OffsetDateTime.parse("2026-03-02T10:00:00+08:00").toInstant().toEpochMilli(),
            assignments.first().dueAt,
        )
    }

    @Test
    fun rejectsNonBooleanReleasedAndMalformedIds() {
        val nonBoolean = "{\"data\":[" + row("a-1", "HW", false, "\"not_submitted\"", "null")
            .replace("\"is_released\":false", "\"is_released\":1") + "]}"
        val badId = "{\"data\":[" + row("a 1", "HW", true, "\"not_submitted\"", "\"2026-03-02T10:00:00+08:00\"") + "]}"

        assertThrows(GeniusTaInvalidResponseException::class.java) {
            GeniusTaParser.assignments(nonBoolean, course)
        }
        assertThrows(GeniusTaInvalidResponseException::class.java) {
            GeniusTaParser.assignments(badId, course)
        }
    }

    @Test
    fun missingUngroupedScheduleFallsBackToAssignmentAndFails() {
        val payload = "{\"data\":[{\"public_id\":\"a-1\",\"title\":\"HW\"," +
            "\"is_released\":true,\"submission_state\":{\"status\":\"not_submitted\"}}]}"

        assertThrows(GeniusTaInvalidResponseException::class.java) {
            GeniusTaParser.assignments(payload, course)
        }
    }

    private fun row(id: String, title: String, released: Boolean, status: String, due: String): String =
        "{\"public_id\":\"$id\",\"title\":\"$title\",\"is_released\":$released," +
            "\"submission_state\":{\"status\":$status},\"ungrouped_schedule\":{\"due_date\":$due}}"
}
