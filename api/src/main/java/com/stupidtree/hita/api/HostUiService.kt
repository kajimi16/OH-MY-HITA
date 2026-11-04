package com.stupidtree.hita.api

import android.content.Context

/**
 * 宿主提供给插件的跨域跳转能力。
 *
 * 迁移期用：课表插件已经搬出宿主，但课表详情、教务导入等页面还在宿主里，
 * 插件通过这个契约请求宿主打开它们，而不是直接 Intent 到宿主的 Activity。
 */
interface HostUiService {
    fun openTimetableDetail(context: Context, timetableId: String)
    fun openImportTimetable(context: Context)

    companion object {
        val KEY = ServiceKey<HostUiService>("hita.host.ui")
    }
}
