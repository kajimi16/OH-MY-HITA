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

    override fun openDetail(context: Context, timetableId: String) {
        context.startActivity(
            Intent(context, com.stupidtree.hitax.ui.timetable.detail.TimetableDetailActivity::class.java)
                .putExtra("id", timetableId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
