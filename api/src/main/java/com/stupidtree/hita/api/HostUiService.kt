package com.stupidtree.hita.api

import android.content.Context
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.TermSubject
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.data.model.timetable.Timetable

/**
 * 宿主提供给插件的跨域跳转能力。
 *
 * 迁移期用：课表插件已经搬出宿主，但课表详情、教务导入等页面还在宿主里，
 * 插件通过这个契约请求宿主打开它们，而不是直接 Intent 到宿主的 Activity。
 */
interface HostUiService {
    fun openTimetableDetail(context: Context, timetableId: String)
    fun openImportTimetable(context: Context)
    fun openCourseResourceSearch(context: Context, keyword: String?, submit: Boolean)
    fun openTeacherHomepage(context: Context, teacherName: String)
    /**
     * 打开"添加事件"弹窗；[timetable] 可为空。
     * [dow]/[period] 非空时会预填时间。
     */
    fun openAddEvent(
        activity: android.app.Activity,
        timetable: Timetable?,
        dow: Int = -1,
        week: Int = 1,
        period: TimePeriodInDay? = null,
        subject: TermSubject? = null,
    )

    /** 教师名自动补全（宿主持有教师检索仓库）。 */
    fun searchTeacherNames(query: String): androidx.lifecycle.LiveData<List<String>>

    /** 通知宿主刷新桌面小组件。 */
    fun notifyWidgets()

    /** 立刻触发一次教务课表自动导入（宿主持有 EAS 账号与仓库）。 */
    fun startAutoReimport(callback: (Boolean) -> Unit)

    /** 打开课程资料 README（宿主持有 HOA 仓库）。 */
    fun openCourseReadme(
        context: Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        courseCode: String?,
        courseName: String?,
    )

    /** 打开课程资料投稿页。 */
    fun openCourseContribution(
        context: Context,
        repoName: String,
        courseName: String,
        courseCode: String,
        repoType: String,
    )

    /** 打开个人主页（用户插件实现）。 */
    fun openUserProfile(context: Context, userId: String?)

    /** 展示事件详情弹窗（宿主负责渲染）。 */
    fun showEventInfo(context: Context, events: List<EventItem>)

    companion object {
        val KEY = ServiceKey<HostUiService>("hita.host.ui")
    }
}
