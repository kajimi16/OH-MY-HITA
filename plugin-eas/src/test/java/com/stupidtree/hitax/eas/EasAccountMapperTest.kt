package com.stupidtree.hitax.eas

import com.stupidtree.hita.api.dto.DtoJson
import com.stupidtree.hitax.data.model.eas.EASToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EasAccountMapperTest {

    @Test
    fun loggedOutTokenMapsToEmptySnapshot() {
        val dto = EASToken().toAccountDto()
        assertFalse(dto.loggedIn)
        assertFalse(dto.hasApiToken)
        assertNull(dto.displayName)
        assertNull(dto.subtitle)
        assertNull(dto.studentId)
        assertNull(dto.avatarUrl)
    }

    @Test
    fun apiTokenMapsIdentityWithoutCredentials() {
        val token = EASToken().apply {
            accessToken = "bearer-value"
            username = "20230001"
            name = "张三"
            stuId = "20230001"
            picture = "https://example.test/a.png"
            stutype = EASToken.TYPE.UNDERGRAD
            school = "计算机学院"
            major = "软件工程"
            grade = "2023"
            password = "secret"
            cookies["JSESSIONID"] = "cookie"
        }

        val dto = token.toAccountDto()
        assertTrue(dto.loggedIn)
        assertTrue(dto.hasApiToken)
        assertEquals("张三", dto.displayName)
        assertEquals("20230001", dto.subtitle)
        assertEquals("20230001", dto.studentId)
        assertEquals("https://example.test/a.png", dto.avatarUrl)
        assertEquals("UNDERGRAD", dto.studentType)
        assertEquals("计算机学院", dto.school)
        assertEquals("软件工程", dto.major)
        assertEquals("2023", dto.grade)

        val json = DtoJson.toJson(dto)
        listOf("bearer-value", "secret", "JSESSIONID", "accessToken", "password", "cookies")
            .forEach { secret -> assertFalse("must not leak $secret", json.contains(secret)) }
    }

    @Test
    fun webCookieSessionIsLoggedInButHasNoApiToken() {
        val token = EASToken().apply {
            username = "20230001"
            webCookies["JSESSIONID"] = "web-cookie"
        }
        val dto = token.toAccountDto()
        assertTrue(dto.loggedIn)
        assertFalse(dto.hasApiToken)
    }

    @Test
    fun subtitleIsNullWhenStudentIdMissing() {
        val token = EASToken().apply {
            accessToken = "bearer-value"
            username = "20230001"
            name = "张三"
            stuId = null
        }
        assertNull(token.toAccountDto().subtitle)
    }

    @Test
    fun subtitleFallsBackToUsernameWhenStudentIdBlank() {
        val token = EASToken().apply {
            accessToken = "bearer-value"
            username = "20230001"
            stuId = " "
        }
        assertEquals("20230001", token.toAccountDto().subtitle)
    }

    @Test
    fun displayNameFallsBackToUsername() {
        val token = EASToken().apply {
            accessToken = "bearer-value"
            username = "20230001"
            name = " "
        }
        assertEquals("20230001", token.toAccountDto().displayName)
    }
}
