package com.stupidtree.hitax.eas

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.stupidtree.hita.api.EasAccountService
import com.stupidtree.hita.api.EasUiService
import com.stupidtree.hita.api.dto.EasImportOptionsDto
import com.stupidtree.hita.api.dto.EasLoginRequestDto
import com.stupidtree.hita.api.dto.EasLoginResultDto
import com.stupidtree.hitax.ui.eas.classroom.EmptyClassroomActivity
import com.stupidtree.hitax.ui.eas.exam.ExamActivity
import com.stupidtree.hitax.ui.eas.imp.ImportTimetableActivity
import com.stupidtree.hitax.ui.eas.login.PopUpLoginEAS
import com.stupidtree.hitax.ui.eas.score.ScoreInquiryActivity
import java.util.concurrent.atomic.AtomicBoolean

/** 页面目标；测试用假 launcher 不需要 Activity。 */
internal enum class EasUiTarget {
    IMPORT_TIMETABLE,
    SCORES,
    CLASSROOM,
    EXAM,
}

internal interface EasUiLauncher {
    /** 显示登录窗；onResult 只由本类去重。 */
    fun showLogin(activity: FragmentActivity, onResult: (Boolean) -> Unit)

    fun open(activity: FragmentActivity, target: EasUiTarget, autoImport: Boolean = false)
}

internal class AndroidEasUiLauncher : EasUiLauncher {
    override fun showLogin(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
        val window = PopUpLoginEAS()
        window.onResponseListener = object : PopUpLoginEAS.OnResponseListener {
            override fun onSuccess(window: PopUpLoginEAS) {
                window.dismiss()
                onResult(true)
            }

            override fun onFailed(window: PopUpLoginEAS) {
                onResult(false)
            }
        }
        window.show(activity.supportFragmentManager, "verify")
    }

    override fun open(activity: FragmentActivity, target: EasUiTarget, autoImport: Boolean) {
        val intent = when (target) {
            EasUiTarget.IMPORT_TIMETABLE ->
                Intent(activity, ImportTimetableActivity::class.java).putExtra("autoImport", autoImport)
            EasUiTarget.SCORES -> Intent(activity, ScoreInquiryActivity::class.java)
            EasUiTarget.CLASSROOM -> Intent(activity, EmptyClassroomActivity::class.java)
            EasUiTarget.EXAM -> Intent(activity, ExamActivity::class.java)
        }
        activity.startActivity(intent)
    }
}

/**
 * 页面入口实现。行为与旧 `EasServiceImpl` 一致：已登录直接执行；未登录先弹登录窗。
 */
internal class EasUiServiceImpl(
    private val accountService: EasAccountService,
    private val launcher: EasUiLauncher = AndroidEasUiLauncher(),
) : EasUiService {

    override fun ensureLogin(
        activity: FragmentActivity,
        request: EasLoginRequestDto,
        callback: (EasLoginResultDto) -> Unit,
    ) {
        ensureLoginCore({ onResult -> launcher.showLogin(activity, onResult) }, callback)
    }

    override fun openAccount(activity: FragmentActivity) {
        openAccountCore { onResult -> launcher.showLogin(activity, onResult) }
    }

    /**
     * 旧实现是 `ensureLogin(activity, onSuccess = {}, onFailed = {})`：
     * 只确保登录，不打开任何账号页。这里保持同语义，并抽成无 Activity 的核心便于测试。
     */
    internal fun openAccountCore(showLogin: (onResult: (Boolean) -> Unit) -> Unit) {
        ensureLoginCore(showLogin) { }
    }

    override fun openImportTimetable(activity: FragmentActivity, options: EasImportOptionsDto) {
        ensureLogin(activity, EasLoginRequestDto()) { result ->
            if (result.success) launcher.open(activity, EasUiTarget.IMPORT_TIMETABLE, options.autoImport)
        }
    }

    override fun openScores(activity: FragmentActivity) {
        ensureLogin(activity, EasLoginRequestDto()) { result ->
            if (result.success) launcher.open(activity, EasUiTarget.SCORES)
        }
    }

    override fun openClassroom(activity: FragmentActivity) {
        ensureLogin(activity, EasLoginRequestDto()) { result ->
            if (result.success) launcher.open(activity, EasUiTarget.CLASSROOM)
        }
    }

    override fun openExam(activity: FragmentActivity) {
        ensureLogin(activity, EasLoginRequestDto()) { result ->
            if (result.success) launcher.open(activity, EasUiTarget.EXAM)
        }
    }

    /**
     * 无 Activity 的登录决策核心，便于纯 JVM 测试。
     * 已登录同步回调；未登录把 `showLogin` 的结果去重后回调一次。
     */
    internal fun ensureLoginCore(
        showLogin: (onResult: (Boolean) -> Unit) -> Unit,
        callback: (EasLoginResultDto) -> Unit,
    ) {
        val account = accountService.account()
        if (account.loggedIn) {
            callback(EasLoginResultDto(success = true, account = account))
            return
        }

        val finished = AtomicBoolean(false)
        fun finish(success: Boolean) {
            if (!finished.compareAndSet(false, true)) return
            callback(
                EasLoginResultDto(
                    success = success,
                    cancelled = false,
                    errorCode = if (success) null else "LOGIN_FAILED",
                    account = if (success) accountService.account() else null,
                )
            )
        }
        showLogin { finish(it) }
    }
}
