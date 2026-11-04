package com.stupidtree.hita.api

import android.content.Context

/** 课表插件的界面入口。宿主只认这个契约，不 import 插件里的 Activity。 */
interface TimetableUiService {
    fun openManager(context: Context)
    fun openDetail(context: Context, timetableId: String)
    fun openSubject(context: Context, subjectId: String)

    /** 打开课表样式面板（插件内的 BottomSheet） */
    fun openStylePanel(activity: androidx.fragment.app.FragmentActivity)

    companion object {
        val KEY = ServiceKey<TimetableUiService>("hita.timetable.ui")
    }
}
