package com.stupidtree.hitax.ui.widgets

import android.content.Context
import android.content.Intent
import com.stupidtree.hitax.ui.widgets.today.normal.TodayWidget
import com.stupidtree.hitax.ui.widgets.today.slim.TodayWidgetSlim

object WidgetUtils {
    val widgets = listOf(TodayWidget::class.java, TodayWidgetSlim::class.java)
    const val EVENT_REFRESH = "com.stupidtree.hita.WIDGET_EVENT_REFRESH"

    fun sendRefreshToAll(context: Context){
        for(wid in widgets){
            val btIntent = Intent().setAction(EVENT_REFRESH)
            btIntent.setClass(context, wid)
            context.sendBroadcast(btIntent)
        }
        // Lite widgets live in the bundled legacy UI module; refresh them explicitly
        // because they are not part of this plugin's class list.
        for (name in LITE_WIDGETS) {
            runCatching {
                context.sendBroadcast(
                    Intent(EVENT_REFRESH).setClassName(context.packageName, name)
                )
            }
        }
    }

    private val LITE_WIDGETS = listOf(
        "com.stupidtree.hitax.legacy.lite.ui.widgets.today.LiteTodayWidget",
        "com.stupidtree.hitax.legacy.lite.ui.widgets.today.LiteTodayWidgetSlim",
    )
}