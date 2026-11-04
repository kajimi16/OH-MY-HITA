package com.stupidtree.hita.api

import android.content.Context
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
    fun openSubject(context: Context, subjectId: String)
    fun openCourseResourceSearch(context: Context, keyword: String?, submit: Boolean)
    fun openTeacherHomepage(context: Context, teacherName: String)
    /** 打开"添加事件"弹窗；[timetable] 可为空，由用户自行选择 */
    fun openAddEvent(activity: android.app.Activity, timetable: Timetable?)

    companion object {
        val KEY = ServiceKey<HostUiService>("hita.host.ui")
    }
}
