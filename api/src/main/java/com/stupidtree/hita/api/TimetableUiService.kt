package com.stupidtree.hita.api

import android.content.Context
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.TermSubject
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.data.model.timetable.Timetable

/** 课表插件的界面入口。宿主只认这个契约，不 import 插件里的 Activity。 */
interface TimetableUiService {
    fun openManager(context: Context)
    fun openDetail(context: Context, timetableId: String)
    fun openSubject(context: Context, subjectId: String)

    /** 事件详情弹窗（插件实现，宿主只转发） */
    fun showEventInfo(activity: androidx.fragment.app.FragmentActivity, events: List<EventItem>)

    /** 添加事件弹窗 */
    fun openAddEvent(
        activity: android.app.Activity,
        timetable: Timetable?,
        dow: Int = -1,
        week: Int = 1,
        period: TimePeriodInDay? = null,
        subject: TermSubject? = null,
    )

    /** 打开课表样式面板（插件内的 BottomSheet） */
    fun openStylePanel(activity: androidx.fragment.app.FragmentActivity)

    /** 请求刷新桌面小组件（小组件实现位于课表插件内）。 */
    fun refreshWidgets(context: Context)

    companion object {
        val KEY = ServiceKey<TimetableUiService>("hita.timetable.ui")
    }
}
