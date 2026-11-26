package com.stupidtree.hitax.eas

import com.stupidtree.hita.api.dto.DtoJson
import com.stupidtree.hitax.data.model.eas.EASToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EasAccountServiceImplTest {

    @Test
    fun loggedOutSnapshotHasNoBusinessData() {
        val dto = EasAccountServiceImpl { EASToken() }.account()
        assertFalse(dto.loggedIn)
        assertFalse(dto.hasApiToken)
        assertNull(dto.displayName)
        assertNull(dto.subtitle)
        assertNull(dto.studentId)
        assertNull(dto.avatarUrl)
    }

    @Test
    fun bearerTokenSnapshotUsesLegacyLoginSemantics() {
        val token = EASToken().apply {
            accessToken = "bearer-value"
            username = "20230001"
            name = "张三"
            stuId = "20230001"
            picture = "https://example.test/a.png"
            school = "计算机学院"
            major = "软件工程"
            grade = "2023"
        }
        val dto = EasAccountServiceImpl { token }.account()
        assertTrue(dto.loggedIn)
        assertTrue(dto.hasApiToken)
        assertEquals("张三", dto.displayName)
        assertEquals("20230001", dto.subtitle)
        assertEquals("20230001", dto.studentId)
        assertEquals("https://example.test/a.png", dto.avatarUrl)
    }

    @Test
    fun webCookieOnlySessionIsLoggedInWithoutApiToken() {
        val token = EASToken().apply {
            username = "20230001"
            webCookies["JSESSIONID"] = "web-cookie"
        }
        val dto = EasAccountServiceImpl { token }.account()
        assertTrue(dto.loggedIn)
        assertFalse(dto.hasApiToken)
    }

    @Test
    fun snapshotNeverCarriesCredentials() {
        val token = EASToken().apply {
            accessToken = "bearer-value"
            password = "secret"
            cookies["JSESSIONID"] = "cookie"
            webCookies["JSESSIONID"] = "web-cookie"
        }
        val json = DtoJson.toJson(EasAccountServiceImpl { token }.account())
        listOf("bearer-value", "secret", "JSESSIONID", "accessToken", "password", "cookies")
            .forEach { secret -> assertFalse("must not leak $secret", json.contains(secret)) }
    }
}
