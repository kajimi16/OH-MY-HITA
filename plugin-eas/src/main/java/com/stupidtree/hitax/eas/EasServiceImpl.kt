package com.stupidtree.hitax.eas

import android.app.Application
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.stupidtree.hita.api.EasService
import com.stupidtree.hitax.data.repository.EASRepository
import com.stupidtree.hitax.data.repository.EasSettingsRepository
import com.stupidtree.hitax.data.model.eas.EASToken
import com.stupidtree.hitax.ui.eas.classroom.EmptyClassroomActivity
import com.stupidtree.hitax.ui.eas.exam.ExamActivity
import com.stupidtree.hitax.ui.eas.imp.ImportTimetableActivity
import com.stupidtree.hitax.ui.eas.login.PopUpLoginEAS
import com.stupidtree.hitax.ui.eas.score.ScoreInquiryActivity

class EasServiceImpl(private val application: Application) : EasService {

    private val repo get() = EASRepository.getInstance(application)
    private val settings get() = EasSettingsRepository.getInstance(application)
    private val token: EASToken get() = repo.getEasToken()

    override fun isLogin(): Boolean = token.isLogin()

    override fun accountName(): String? {
        if (!isLogin()) return null
        return token.name?.ifBlank { token.username } ?: token.username
    }

    override fun accountSubtitle(): String? {
        if (!isLogin()) return ""
        return token.stuId?.ifBlank { token.username } ?: ""
    }

    override fun currentStuId(): String? = if (isLogin()) token.stuId else null

    override fun accountAvatarUrl(): String? = if (isLogin()) token.picture else null

    override fun logout() {
        repo.logout()
    }

    override fun ensureLogin(activity: FragmentActivity, onSuccess: () -> Unit, onFailed: () -> Unit) {
        if (isLogin()) {
            onSuccess()
            return
        }
        val window = PopUpLoginEAS()
        window.onResponseListener = object : PopUpLoginEAS.OnResponseListener {
            override fun onSuccess(window: PopUpLoginEAS) {
                window.dismiss()
                onSuccess()
            }

            override fun onFailed(window: PopUpLoginEAS) {
                onFailed()
            }
        }
        window.show(activity.supportFragmentManager, "verify")
    }

    override fun openImportTimetable(activity: FragmentActivity, autoImport: Boolean) {
        ensureLogin(activity, onSuccess = {
            activity.startActivity(
                Intent(activity, ImportTimetableActivity::class.java).putExtra("autoImport", autoImport)
            )
        })
    }

    override fun openScores(activity: FragmentActivity) =
        ensureLogin(activity, onSuccess = { activity.startActivity(Intent(activity, ScoreInquiryActivity::class.java)) })

    override fun openClassroom(activity: FragmentActivity) =
        ensureLogin(activity, onSuccess = { activity.startActivity(Intent(activity, EmptyClassroomActivity::class.java)) })

    override fun openExam(activity: FragmentActivity) =
        ensureLogin(activity, onSuccess = { activity.startActivity(Intent(activity, ExamActivity::class.java)) })


    override fun openAccount(activity: FragmentActivity) = ensureLogin(activity, onSuccess = {})

    override fun autoReimportIfDue() {
        if (!settings.isAutoReimportEnabled()) return
        if (!isLogin()) return
        val now = System.currentTimeMillis()
        if (now - settings.getLastAutoReimportTs() < INTERVAL_MS) return
        startAutoReimport { success ->
            if (success) settings.setLastAutoReimportTs(System.currentTimeMillis())
        }
    }

    override fun startAutoReimport(callback: (Boolean) -> Unit) {
        if (!isLogin()) {
            callback(false)
            return
        }
        val isUndergrad = token.stutype == EASToken.TYPE.UNDERGRAD
        repo.startAutoImportCurrentTimetable(isUndergrad) { success ->
            if (success) settings.setLastAutoReimportTs(System.currentTimeMillis())
            callback(success)
        }
    }

    companion object {
        private const val INTERVAL_MS = 12 * 60 * 60 * 1000L
    }
}
