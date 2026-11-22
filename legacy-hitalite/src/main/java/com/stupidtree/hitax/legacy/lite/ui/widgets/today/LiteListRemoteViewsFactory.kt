package com.stupidtree.hitax.legacy.lite.ui.widgets.today

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.utils.TimeTools

/**
 * 今日组件列表工厂。
 *
 * 前置条件：`LiteListWidgetService` 传入带 `EXTRA_APPWIDGET_ID` 的 Intent。
 * 后置条件：`getCount` 与宿主当日事件数一致，每项展示名称、时段、地点。
 * 不变式：数据只来自 `LiteHostBridge.timetable.getTodayEventsSync()`。
 * 违约处理：宿主服务缺失时返回空列表。
 */
internal open class LiteListRemoteViewsFactory(
    private val context: Context,
    intent: Intent,
    private val slim: Boolean = false
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    private val events = mutableListOf<EventItem>()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val today = LiteTodayUtils.todayEvents().orEmpty()
        events.clear()
        events.addAll(today)
    }

    override fun onDestroy() {
        events.clear()
    }

    override fun getCount(): Int = events.size

    override fun getViewAt(position: Int): RemoteViews {
        check(position in events.indices) { "Invalid widget item position: $position" }
        val event = events[position]
        val dark = LiteTodayUtils.isWidgetDarkTheme(context)
        val views = RemoteViews(
            context.packageName,
            if (slim) R.layout.lite_widget_today_item_slim else R.layout.lite_widget_today_item
        )
        views.setTextViewText(R.id.lite_widget_item_name, event.name)
        views.setTextViewText(
            R.id.lite_widget_item_time,
            TimeTools.printTime(event.from.time) + "-" + TimeTools.printTime(event.to.time)
        )
        views.setTextViewText(
            R.id.lite_widget_item_place,
            if (TextUtils.isEmpty(event.place)) {
                context.getString(R.string.lite_widget_unknown_location)
            } else {
                event.place
            }
        )
        views.setInt(
            R.id.lite_widget_item_place_wrap,
            "setBackgroundResource",
            if (dark) R.drawable.lite_widget_rounded_bar_dark else R.drawable.lite_widget_rounded_bar
        )
        views.setTextColor(
            R.id.lite_widget_item_name,
            context.getColor(
                if (dark) R.color.lite_widget_item_name_dark else R.color.lite_widget_item_name_light
            )
        )
        views.setTextColor(
            R.id.lite_widget_item_time,
            context.getColor(
                if (dark) R.color.lite_widget_item_time_dark else R.color.lite_widget_item_time_light
            )
        )
        views.setTextColor(
            R.id.lite_widget_item_place,
            context.getColor(
                if (dark) R.color.lite_widget_item_place_dark else R.color.lite_widget_item_place_light
            )
        )
        views.setInt(
            R.id.lite_widget_item_clock,
            "setBackgroundResource",
            if (dark) R.drawable.lite_widget_ic_clock_white else R.drawable.lite_widget_ic_clock
        )
        views.setInt(
            R.id.lite_widget_item_place_icon,
            "setBackgroundResource",
            if (dark) R.drawable.lite_widget_ic_location_white else R.drawable.lite_widget_ic_location
        )
        val fillInIntent = Intent().putExtras(
            Bundle().apply {
                putString(LiteTodayUtils.EXTRA_EVENT_ID, event.id)
                putSerializable(LiteTodayUtils.EXTRA_EVENT, event)
                putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
        )
        views.setOnClickFillInIntent(R.id.lite_widget_item, fillInIntent)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false
}
