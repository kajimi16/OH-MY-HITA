package com.stupidtree.hita.api

import androidx.fragment.app.FragmentActivity

/**
 * 教务能力契约。
 *
 * 宿主（功能中心卡片、抽屉、关于页、自动导入）只依赖这个接口；
 * 登录弹窗、成绩/空教室/考试/导入页面、会话与抓取全部在 `:plugin-eas`。
 */
interface EasService {

    fun isLogin(): Boolean

    /** 已登录账号的显示名；未登录返回 null。 */
    fun accountName(): String?

    /** 账号副标题（学号等）；未登录返回空串。 */
    fun accountSubtitle(): String?

    fun currentStuId(): String?

    fun logout()

    fun accountAvatarUrl(): String?

    /** 已登录直接执行；否则弹登录窗，成功后执行 [onSuccess]。 */
    fun ensureLogin(activity: FragmentActivity, onSuccess: () -> Unit, onFailed: () -> Unit = {})

    fun openImportTimetable(activity: FragmentActivity, autoImport: Boolean = false)

    fun openScores(activity: FragmentActivity)

    fun openClassroom(activity: FragmentActivity)

    fun openExam(activity: FragmentActivity)

    /** 考试列表；未登录返回 NOT_LOGGED_IN。 */
    fun exams(): androidx.lifecycle.LiveData<com.stupidtree.component.data.DataState<List<com.stupidtree.hitax.data.model.eas.ExamItem>>>

    /** 打开登录/账号面板。 */
    fun openAccount(activity: FragmentActivity)

    /** 到达间隔时自动导入当前学期课表（异步）。 */
    fun autoReimportIfDue()

    fun startAutoReimport(callback: (Boolean) -> Unit)

    companion object {
        val KEY = ServiceKey<EasService>("hita.eas")
    }
}
