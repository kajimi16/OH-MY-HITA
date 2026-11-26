package com.stupidtree.hita.api

import androidx.fragment.app.FragmentActivity
import com.stupidtree.hita.api.dto.EasImportOptionsDto
import com.stupidtree.hita.api.dto.EasLoginRequestDto
import com.stupidtree.hita.api.dto.EasLoginResultDto

/**
 * 教务页面入口（API 14）。
 *
 * 页面跳转从 `EasService` 分出，但仍允许 `FragmentActivity`：
 * 登录弹窗需要 `supportFragmentManager`，页面由插件内的 Activity 承载。
 */
interface EasUiService {
    /**
     * 确保“API 可用登录”：`hasApiToken = true` 才直接回调成功；
     * 只有 WebView cookie（`loggedIn = true` 但 `hasApiToken = false`）仍会弹登录窗。
     * callback 恰好一次。
     */
    fun ensureLogin(
        activity: FragmentActivity,
        request: EasLoginRequestDto = EasLoginRequestDto(),
        callback: (EasLoginResultDto) -> Unit,
    )

    /**
     * 打开登录/账号面板；只确保任意登录态（`loggedIn`），不要求 API token。
     * 已有 WebView cookie 会话时不会重复弹窗。
     */
    fun openAccount(activity: FragmentActivity)

    fun openImportTimetable(
        activity: FragmentActivity,
        options: EasImportOptionsDto = EasImportOptionsDto(),
    )

    fun openScores(activity: FragmentActivity)

    fun openClassroom(activity: FragmentActivity)

    fun openExam(activity: FragmentActivity)

    companion object {
        val KEY = ServiceKey<EasUiService>("hita.eas.ui")
    }
}
