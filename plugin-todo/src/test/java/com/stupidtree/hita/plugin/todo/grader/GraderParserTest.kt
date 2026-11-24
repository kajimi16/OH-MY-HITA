package com.stupidtree.hita.plugin.todo.grader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime

class GraderParserTest {

    @Test
    fun accountParsesIdentityAndFallsBackToUsername() {
        assertEquals(
            GraderAccount("12", "Alice"),
            GraderParser.account("{\"id\":12,\"username\":\"alice\",\"real_name\":\"Alice\"}"),
        )
        assertEquals(
            GraderAccount("13", "bob"),
            GraderParser.account("{\"id\":13,\"username\":\"bob\",\"real_name\":\"  \"}"),
        )
    }

    @Test
    fun accountRejectsInvalidIdsAndMissingUsername() {
        listOf(
            "{\"id\":0,\"username\":\"a\"}",
            "{\"id\":-1,\"username\":\"a\"}",
            "{\"id\":1.5,\"username\":\"a\"}",
            "{\"id\":\"1\",\"username\":\"a\"}",
            "{\"id\":1,\"username\":\"  \"}",
            "{}",
        ).forEach { payload ->
            assertThrows(GraderInvalidResponseException::class.java) { GraderParser.account(payload) }
        }
    }

    @Test
    fun coursesParseAndRejectDuplicateIds() {
        assertEquals(
            listOf(GraderCourse("7", "Math"), GraderCourse("8", "Physics")),
            GraderParser.courses("[{\"id\":7,\"title\":\"Math\"},{\"id\":8,\"title\":\"Physics\"}]"),
        )
        assertThrows(GraderInvalidResponseException::class.java) {
            GraderParser.courses("[{\"id\":7,\"title\":\"Math\"},{\"id\":7,\"title\":\"Physics\"}]")
        }
    }

    @Test
    fun assignmentsParseDeadlineAndTolerateMalformedDeadline() {
        val course = GraderCourse("7", "Math")
        val payload = "[{\"id\":3,\"title\":\"HW1\",\"course\":7,\"published\":true,\"due_time\":\"2026-03-01T12:00:00+08:00\"}]"
        val assignment = GraderParser.assignments(payload, course).single()

        assertEquals("3", assignment.id)
        assertEquals("HW1", assignment.title)
        assertEquals("Math", assignment.courseTitle)
        assertEquals(OffsetDateTime.parse("2026-03-01T12:00:00+08:00").toInstant().toEpochMilli(), assignment.dueAt)
        assertTrue(assignment.published)

        val malformed = "[{\"id\":3,\"title\":\"HW1\",\"course\":7,\"published\":true,\"due_time\":\"not-a-date\"}]"
        assertNull(GraderParser.assignments(malformed, course).single().dueAt)
        val missing = "[{\"id\":3,\"title\":\"HW1\",\"course\":7,\"published\":true}]"
        assertNull(GraderParser.assignments(missing, course).single().dueAt)
    }

    @Test
    fun assignmentsRejectWrongCourseAndNonBooleanPublished() {
        val course = GraderCourse("7", "Math")
        listOf(
            "[{\"id\":3,\"title\":\"HW\",\"course\":8,\"published\":true}]",
            "[{\"id\":3,\"title\":\"HW\",\"course\":7,\"published\":1}]",
            "[{\"id\":3,\"title\":\"HW\",\"course\":7}]",
            "[{\"id\":0,\"title\":\"HW\",\"course\":7,\"published\":true}]",
        ).forEach { payload ->
            assertThrows(GraderInvalidResponseException::class.java) {
                GraderParser.assignments(payload, course)
            }
        }
    }

    @Test
    fun submissionsAcceptNumericIdsOnly() {
        assertEquals(
            setOf("5", "9"),
            GraderParser.submittedAssignmentIds("[{\"assignment\":5},{\"assignment\":9}]"),
        )
        listOf(
            "[{\"assignment\":0}]",
            "[{\"assignment\":-2}]",
            "[{\"assignment\":\"5\"}]",
            "[{}]",
        ).forEach { payload ->
            assertThrows(GraderInvalidResponseException::class.java) {
                GraderParser.submittedAssignmentIds(payload)
            }
        }
    }

    @Test
    fun uniqueCollapsesIdenticalRowsAndRejectsConflicts() {
        val row = GraderAssignment("3", "HW", "Math", null, true)
        assertEquals(listOf(row), GraderParser.unique(listOf(row, row)))
        assertThrows(GraderInvalidResponseException::class.java) {
            GraderParser.unique(listOf(row, row.copy(title = "Other")))
        }
    }
}
