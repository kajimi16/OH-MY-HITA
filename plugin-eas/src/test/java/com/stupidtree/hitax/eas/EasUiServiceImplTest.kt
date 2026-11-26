package com.stupidtree.hitax.eas

import androidx.fragment.app.FragmentActivity
import com.stupidtree.hita.api.EasAccountService
import com.stupidtree.hita.api.dto.EasAccountDto
import com.stupidtree.hita.api.dto.EasLoginResultDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EasUiServiceImplTest {

    private class FakeAccountService(var dto: EasAccountDto) : EasAccountService {
        override fun account(): EasAccountDto = dto
    }

    private class FakeLauncher : EasUiLauncher {
        var shown = 0
        var opened = mutableListOf<EasUiTarget>()

        override fun showLogin(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
            shown++
        }

        override fun open(activity: FragmentActivity, target: EasUiTarget, autoImport: Boolean) {
            opened += target
        }
    }

    @Test
    fun apiTokenCompletesSynchronouslyWithoutPrompt() {
        val account = FakeAccountService(
            EasAccountDto(
                loggedIn = true,
                hasApiToken = true,
                studentId = "20230001",
                displayName = "张三",
            )
        )
        val launcher = FakeLauncher()
        val results = mutableListOf<EasLoginResultDto>()

        EasUiServiceImpl(account, launcher).ensureLoginCore(
            showLogin = { error("must not show login") },
            callback = { results += it },
        )

        assertEquals(1, results.size)
        assertTrue(results.single().success)
        assertEquals("20230001", results.single().account?.studentId)
        assertEquals(0, launcher.shown)
    }

    @Test
    fun webOnlySessionMustShowLoginInsteadOfEarlySuccess() {
        val account = FakeAccountService(
            EasAccountDto(loggedIn = true, hasApiToken = false, studentId = "20230001")
        )
        val launcher = FakeLauncher()
        val results = mutableListOf<EasLoginResultDto>()
        var shown = 0

        EasUiServiceImpl(account, launcher).ensureLoginCore(
            showLogin = { onResult ->
                shown++
                account.dto = EasAccountDto(
                    loggedIn = true,
                    hasApiToken = true,
                    studentId = "20230001",
                )
                onResult(true)
            },
            callback = { results += it },
        )

        assertEquals("Web-only 会话必须弹登录窗", 1, shown)
        assertEquals(1, results.size)
        assertTrue(results.single().success)
    }

    @Test
    fun failedPromptCompletesExactlyOnce() {
        val account = FakeAccountService(EasAccountDto(loggedIn = false))
        val launcher = FakeLauncher()
        val results = mutableListOf<EasLoginResultDto>()

        EasUiServiceImpl(account, launcher).ensureLoginCore(
            showLogin = { onResult ->
                onResult(false)
                onResult(true)
            },
            callback = { results += it },
        )

        assertEquals(1, results.size)
        assertFalse(results.single().success)
        assertEquals("LOGIN_FAILED", results.single().errorCode)
        assertNull(results.single().account)
    }

    @Test
    fun successfulPromptUsesFreshAccountSnapshot() {
        val account = FakeAccountService(EasAccountDto(loggedIn = false))
        val launcher = FakeLauncher()
        val results = mutableListOf<EasLoginResultDto>()

        EasUiServiceImpl(account, launcher).ensureLoginCore(
            showLogin = { onResult ->
                account.dto = EasAccountDto(
                    loggedIn = true,
                    hasApiToken = true,
                    studentId = "20230001",
                )
                onResult(true)
            },
            callback = { results += it },
        )

        assertEquals(1, results.size)
        assertTrue(results.single().success)
        assertEquals("20230001", results.single().account?.studentId)
    }

    @Test
    fun openAccountOnlyEnsuresLoginAndDoesNotNavigate() {
        val account = FakeAccountService(EasAccountDto(loggedIn = false))
        val launcher = FakeLauncher()
        var shown = 0

        EasUiServiceImpl(account, launcher).openAccountCore { onResult ->
            shown++
            onResult(false)
        }

        assertEquals(1, shown)
        assertTrue("旧 openAccount 不打开账号页", launcher.opened.isEmpty())
    }

    @Test
    fun openAccountWithWebOnlySessionDoesNotShowOrNavigate() {
        // openAccount 保持旧语义：任意登录态即可，不要求 API token。
        val account = FakeAccountService(EasAccountDto(loggedIn = true, hasApiToken = false))
        val launcher = FakeLauncher()

        EasUiServiceImpl(account, launcher).openAccountCore {
            error("已有 Web 会话时不应再弹登录窗")
        }

        assertTrue(launcher.opened.isEmpty())
    }

    @Test
    fun dismissedLoginDoesNotCompleteCallback() {
        val account = FakeAccountService(EasAccountDto(loggedIn = false))
        val launcher = FakeLauncher()
        val results = mutableListOf<EasLoginResultDto>()

        EasUiServiceImpl(account, launcher).ensureLoginCore(
            showLogin = { /* 用户关闭弹窗：旧实现不回调 onFailed */ },
            callback = { results += it },
        )

        assertTrue(results.isEmpty())
    }

    @Test
    fun launcherTargetsAreStable() {
        assertEquals(
            listOf(
                EasUiTarget.IMPORT_TIMETABLE,
                EasUiTarget.SCORES,
                EasUiTarget.CLASSROOM,
                EasUiTarget.EXAM,
            ),
            EasUiTarget.values().toList(),
        )
    }
}
