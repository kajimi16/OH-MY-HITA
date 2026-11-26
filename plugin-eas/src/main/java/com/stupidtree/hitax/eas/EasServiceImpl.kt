package com.stupidtree.hitax.eas

import androidx.fragment.app.FragmentActivity
import com.stupidtree.hita.api.EasAccountService
import com.stupidtree.hita.api.EasService
import com.stupidtree.hita.api.EasSessionService
import com.stupidtree.hita.api.EasUiService
import com.stupidtree.hita.api.dto.EasImportOptionsDto
import com.stupidtree.hita.api.dto.EasLoginRequestDto

/**
 * 旧 `EasService` 兼容门面。方法签名与行为保持不变，内部委托到三个新实现。
 *
 * 新代码应使用 [EasAccountService] / [EasSessionService] / [EasUiService]；
 * 本类只负责旧 key 的二进制兼容。
 */
class EasServiceImpl(
    private val accountService: EasAccountService,
    private val sessionService: EasSessionService,
    private val uiService: EasUiService,
) : EasService {

    override fun isLogin(): Boolean = accountService.account().loggedIn

    override fun accountName(): String? {
        val account = accountService.account()
        return if (account.loggedIn) account.displayName else null
    }

    override fun accountSubtitle(): String? {
        val account = accountService.account()
        return if (account.loggedIn) account.subtitle ?: "" else ""
    }

    override fun currentStuId(): String? {
        val account = accountService.account()
        return if (account.loggedIn) account.studentId else null
    }

    override fun accountAvatarUrl(): String? {
        val account = accountService.account()
        return if (account.loggedIn) account.avatarUrl else null
    }

    override fun logout() = sessionService.logout()

    override fun ensureLogin(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailed: () -> Unit,
    ) {
        uiService.ensureLogin(activity, EasLoginRequestDto()) { result ->
            if (result.success) onSuccess() else onFailed()
        }
    }

    override fun openImportTimetable(activity: FragmentActivity, autoImport: Boolean) {
        uiService.openImportTimetable(activity, EasImportOptionsDto(autoImport = autoImport))
    }

    override fun openScores(activity: FragmentActivity) = uiService.openScores(activity)

    override fun openClassroom(activity: FragmentActivity) = uiService.openClassroom(activity)

    override fun openExam(activity: FragmentActivity) = uiService.openExam(activity)

    override fun openAccount(activity: FragmentActivity) = uiService.openAccount(activity)

    override fun autoReimportIfDue() = sessionService.autoReimportIfDue()

    override fun startAutoReimport(callback: (Boolean) -> Unit) {
        sessionService.startAutoReimport { result -> callback(result.success) }
    }
}
