package com.stupidtree.hitax.eas

import androidx.fragment.app.FragmentActivity
import com.stupidtree.hita.api.EasAccountService
import com.stupidtree.hita.api.EasService
import com.stupidtree.hita.api.EasSessionService
import com.stupidtree.hita.api.EasUiService
import com.stupidtree.hita.api.dto.EasAccountDto
import com.stupidtree.hita.api.dto.EasImportOptionsDto
import com.stupidtree.hita.api.dto.EasLoginRequestDto
import com.stupidtree.hita.api.dto.EasLoginResultDto
import com.stupidtree.hita.api.dto.EasReimportResultDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EasServiceImplTest {

    private class FakeAccount(var dto: EasAccountDto) : EasAccountService {
        override fun account(): EasAccountDto = dto
    }

    private class FakeSession : EasSessionService {
        var logoutCalls = 0
        var autoCalls = 0
        var listener: ((EasReimportResultDto) -> Unit)? = null

        override fun logout() { logoutCalls++ }
        override fun autoReimportIfDue() { autoCalls++ }
        override fun startAutoReimport(listener: (EasReimportResultDto) -> Unit) {
            this.listener = listener
        }

        fun emit(success: Boolean) {
            listener?.invoke(EasReimportResultDto(success = success))
        }
    }

    private class FakeUi : EasUiService {
        override fun ensureLogin(
            activity: FragmentActivity,
            request: EasLoginRequestDto,
            callback: (EasLoginResultDto) -> Unit,
        ) = error("not used")

        override fun openAccount(activity: FragmentActivity) = error("not used")
        override fun openImportTimetable(activity: FragmentActivity, options: EasImportOptionsDto) =
            error("not used")
        override fun openScores(activity: FragmentActivity) = error("not used")
        override fun openClassroom(activity: FragmentActivity) = error("not used")
        override fun openExam(activity: FragmentActivity) = error("not used")
    }

    private fun service(
        dto: EasAccountDto,
        session: FakeSession = FakeSession(),
    ): Pair<EasService, FakeSession> {
        val impl = EasServiceImpl(FakeAccount(dto), session, FakeUi())
        return impl to session
    }

    @Test
    fun loggedOutKeepsLegacyEmptySubtitleAndNulls() {
        val (eas, _) = service(EasAccountDto(loggedIn = false))
        assertFalse(eas.isLogin())
        assertNull(eas.accountName())
        assertEquals("", eas.accountSubtitle())
        assertNull(eas.currentStuId())
        assertNull(eas.accountAvatarUrl())
    }

    @Test
    fun loggedInMapsLegacyAccountFields() {
        val (eas, _) = service(
            EasAccountDto(
                loggedIn = true,
                displayName = "张三",
                subtitle = "20230001",
                studentId = "20230001",
                avatarUrl = "https://example.test/a.png",
            )
        )
        assertTrue(eas.isLogin())
        assertEquals("张三", eas.accountName())
        assertEquals("20230001", eas.accountSubtitle())
        assertEquals("20230001", eas.currentStuId())
        assertEquals("https://example.test/a.png", eas.accountAvatarUrl())
    }

    @Test
    fun nullSubtitleStillBecomesLegacyEmptyString() {
        val (eas, _) = service(
            EasAccountDto(loggedIn = true, displayName = "张三", subtitle = null)
        )
        assertEquals("", eas.accountSubtitle())
    }

    @Test
    fun commandsDelegateToSessionService() {
        val (eas, session) = service(EasAccountDto(loggedIn = true))
        eas.logout()
        eas.autoReimportIfDue()
        assertEquals(1, session.logoutCalls)
        assertEquals(1, session.autoCalls)
    }

    @Test
    fun autoReimportMapsResultToLegacyBoolean() {
        val (eas, session) = service(EasAccountDto(loggedIn = true))
        val results = mutableListOf<Boolean>()
        eas.startAutoReimport { results += it }

        session.emit(true)
        assertEquals(listOf(true), results)
    }

    @Test
    fun autoReimportFailureMapsToLegacyFalse() {
        val (eas, session) = service(EasAccountDto(loggedIn = true))
        val results = mutableListOf<Boolean>()
        eas.startAutoReimport { results += it }

        session.emit(false)
        assertEquals(listOf(false), results)
    }
}
