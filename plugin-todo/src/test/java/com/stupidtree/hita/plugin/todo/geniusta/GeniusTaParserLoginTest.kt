package com.stupidtree.hita.plugin.todo.geniusta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime

class GeniusTaParserLoginTest {

    @Test
    fun loginParsesStudentSessionAndAbsoluteExpiry() {
        val payload = "{\"data\":{\"session\":{\"id\":\"sess-1\",\"expires_at\":\"2026-03-01T00:00:00+08:00\"}," +
            "\"user\":{\"role\":\"student\",\"public_id\":\"stu-1\"}}}"

        val login = GeniusTaParser.login(payload)

        assertEquals("stu-1", login.accountId)
        assertEquals("sess-1", login.sessionId)
        assertEquals(
            OffsetDateTime.parse("2026-03-01T00:00:00+08:00").toInstant().toEpochMilli(),
            login.expiresAt,
        )
    }

    @Test
    fun loginRejectsNonStudentRoleAsAuthenticationFailure() {
        val payload = "{\"data\":{\"session\":{\"id\":\"sess-1\",\"expires_at\":\"2026-03-01T00:00:00+08:00\"}," +
            "\"user\":{\"role\":\"teacher\",\"public_id\":\"tea-1\"}}}"

        assertThrows(GeniusTaAuthenticationException::class.java) { GeniusTaParser.login(payload) }
    }

    @Test
    fun loginRejectsMissingSessionAndMalformedIds() {
        val noExpiry = "{\"data\":{\"session\":{\"id\":\"sess-1\"}," +
            "\"user\":{\"role\":\"student\",\"public_id\":\"stu-1\"}}}"
        val badSessionId = "{\"data\":{\"session\":{\"id\":\"bad id\",\"expires_at\":\"2026-03-01T00:00:00+08:00\"}," +
            "\"user\":{\"role\":\"student\",\"public_id\":\"stu-1\"}}}"
        val noData = "{\"session\":{}}"

        assertThrows(GeniusTaInvalidResponseException::class.java) { GeniusTaParser.login(noExpiry) }
        assertThrows(GeniusTaInvalidResponseException::class.java) { GeniusTaParser.login(badSessionId) }
        assertThrows(GeniusTaInvalidResponseException::class.java) { GeniusTaParser.login(noData) }
    }

    @Test
    fun coursesKeepOnlyStudentCoursesAndRejectDuplicates() {
        val payload = "{\"data\":[" +
            "{\"current_user_role\":\"student\",\"public_id\":\"c-1\",\"name\":\"Math\",\"current_user_section_id\":null}," +
            "{\"current_user_role\":\"teacher\",\"public_id\":\"c-2\",\"name\":\"Ignored\"}]}"

        assertEquals(listOf(GeniusTaCourse("c-1", "Math", null)), GeniusTaParser.courses(payload))

        val sectioned = "{\"data\":[{\"current_user_role\":\"student\",\"public_id\":\"c-1\"," +
            "\"name\":\"Math\",\"current_user_section_id\":\"sec-1\"}]}"
        assertEquals("sec-1", GeniusTaParser.courses(sectioned).single().sectionId)

        val duplicate = "{\"data\":[" +
            "{\"current_user_role\":\"student\",\"public_id\":\"c-1\",\"name\":\"Math\",\"current_user_section_id\":null}," +
            "{\"current_user_role\":\"student\",\"public_id\":\"c-1\",\"name\":\"Physics\",\"current_user_section_id\":null}]}"
        assertThrows(GeniusTaInvalidResponseException::class.java) { GeniusTaParser.courses(duplicate) }
    }

    @Test
    fun accountIdRequiresStudentPayload() {
        val student = "{\"data\":{\"role\":\"student\",\"public_id\":\"stu-1\"}}"
        val teacher = "{\"data\":{\"role\":\"teacher\",\"public_id\":\"tea-1\"}}"

        assertEquals("stu-1", GeniusTaParser.accountId(student))
        assertThrows(GeniusTaAuthenticationException::class.java) { GeniusTaParser.accountId(teacher) }
    }

    @Test
    fun idsRejectCharactersOutsideThePlatformAlphabet() {
        assertTrue(isGeniusTaId("c-1_A"))
        assertTrue(!isGeniusTaId("c 1"))
        assertTrue(!isGeniusTaId(""))
    }
}
