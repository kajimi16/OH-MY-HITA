package com.stupidtree.hitax.plugin

import android.content.Context
import android.content.Intent
import com.stupidtree.hita.api.TimetableUiService
import com.stupidtree.hitax.ui.timetable.manager.TimetableManagerActivity

/** 课表插件的界面入口实现。详情页目前仍在宿主，通过 HostUiService 转交。 */
class TimetableUiImpl : TimetableUiService {

    override fun openManager(context: Context) {
        context.startActivity(
            Intent(context, TimetableManagerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun openSubject(context: Context, subjectId: String) {
        context.startActivity(
            Intent(context, com.stupidtree.hitax.ui.subject.SubjectActivity::class.java)
                .putExtra("id", subjectId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun showEventInfo(
        activity: androidx.fragment.app.FragmentActivity,
        events: List<com.stupidtree.hitax.data.model.timetable.EventItem>,
    ) {
        com.stupidtree.hitax.ui.event.FragmentTimeInfoSheet
            .newInstance(ArrayList(events))
            .show(activity.supportFragmentManager, "event")
    }

    override fun openAddEvent(
        activity: android.app.Activity,
        timetable: com.stupidtree.hitax.data.model.timetable.Timetable?,
        dow: Int,
        week: Int,
        period: com.stupidtree.hitax.data.model.timetable.TimePeriodInDay?,
        subject: com.stupidtree.hitax.data.model.timetable.TermSubject?,
    ) {
        val popup = com.stupidtree.hitax.ui.event.add.PopupAddEvent(true)
            .setInitTimetable(timetable)
        if (dow >= 0 && period != null) popup.setInitTime(dow, week, period)
        subject?.let { popup.setInitSubject(it) }
        popup.show(
            (activity as androidx.fragment.app.FragmentActivity).supportFragmentManager,
            "add_event"
        )
    }

    override fun openStylePanel(activity: androidx.fragment.app.FragmentActivity) {
        com.stupidtree.hitax.ui.main.timetable.panel.FragmentTimetablePanel()
            .show(activity.supportFragmentManager, "panel")
    }

    override fun refreshWidgets(context: Context) {
        com.stupidtree.hitax.ui.widgets.WidgetUtils.sendRefreshToAll(context)
    }

    override fun openDetail(context: Context, timetableId: String) {
        context.startActivity(
            Intent(context, com.stupidtree.hitax.ui.timetable.detail.TimetableDetailActivity::class.java)
                .putExtra("id", timetableId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
