package com.stupidtree.hitax.plugin

import androidx.fragment.app.FragmentActivity
import com.stupidtree.hita.api.EasService
import com.stupidtree.hita.api.dto.EasReimportResultDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EasLegacyAdaptersTest {

    private class FakeEasService : EasService {
        var loggedIn = true
        var name: String? = "张三"
        var subtitle: String? = "20230001"
        var stuId: String? = "20230001"
        var avatar: String? = "https://example.test/a.png"
        var logoutCalls = 0
        var autoCalls = 0
        var importCallback: ((Boolean) -> Unit)? = null

        override fun isLogin(): Boolean = loggedIn
        override fun accountName(): String? = name
        override fun accountSubtitle(): String? = subtitle
        override fun currentStuId(): String? = stuId
        override fun accountAvatarUrl(): String? = avatar
        override fun logout() { logoutCalls++ }
        override fun autoReimportIfDue() { autoCalls++ }

        override fun ensureLogin(
            activity: FragmentActivity,
            onSuccess: () -> Unit,
            onFailed: () -> Unit,
        ) = error("not used in adapter test")

        override fun openImportTimetable(activity: FragmentActivity, autoImport: Boolean) =
            error("not used in adapter test")
        override fun openScores(activity: FragmentActivity) = error("not used in adapter test")
        override fun openClassroom(activity: FragmentActivity) = error("not used in adapter test")
        override fun openExam(activity: FragmentActivity) = error("not used in adapter test")
        override fun openAccount(activity: FragmentActivity) = error("not used in adapter test")

        override fun startAutoReimport(callback: (Boolean) -> Unit) {
            importCallback = callback
        }

        fun emit(success: Boolean) {
            importCallback?.invoke(success)
        }
    }

    @Test
    fun legacyAccountMapsLoggedInFieldsAndApiTokenProbe() {
        val legacy = FakeEasService()
        val dto = LegacyEasAccountService(legacy) { true }.account()

        assertTrue(dto.loggedIn)
        assertTrue(dto.hasApiToken)
        assertEquals("张三", dto.displayName)
        assertEquals("20230001", dto.subtitle)
        assertEquals("20230001", dto.studentId)
        assertEquals("https://example.test/a.png", dto.avatarUrl)
    }

    @Test
    fun legacyAccountLoggedOutHasNoBusinessFields() {
        val legacy = FakeEasService().apply { loggedIn = false }
        val dto = LegacyEasAccountService(legacy) { true }.account()

        assertFalse(dto.loggedIn)
        assertFalse(dto.hasApiToken)
        assertNull(dto.displayName)
        assertNull(dto.subtitle)
        assertNull(dto.studentId)
        assertNull(dto.avatarUrl)
    }

    @Test
    fun legacyAccountBlankSubtitleBecomesNull() {
        val legacy = FakeEasService().apply { subtitle = "   " }
        assertNull(LegacyEasAccountService(legacy) { false }.account().subtitle)
    }

    @Test
    fun legacySessionDeduplicatesCallbackAndDelegatesCommands() {
        val legacy = FakeEasService()
        val results = mutableListOf<EasReimportResultDto>()
        val adapter = LegacyEasSessionService(legacy)

        adapter.startAutoReimport { results += it }
        legacy.emit(true)
        legacy.emit(false)

        assertEquals(1, results.size)
        assertTrue(results.single().success)

        adapter.logout()
        adapter.autoReimportIfDue()
        assertEquals(1, legacy.logoutCalls)
        assertEquals(1, legacy.autoCalls)
    }
}
