package com.stupidtree.hitax.legacy.lite.ui.widgets.today

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.WorkerThread
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.legacy.lite.HitaLiteActivity
import com.stupidtree.hitax.legacy.lite.LiteHostBridge
import com.stupidtree.hitax.legacy.lite.R
import com.stupidtree.hitax.utils.TimeTools
import com.stupidtree.style.ThemeTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 在广播返回前异步执行刷新，并在结束时释放 `PendingResult`。
 *
 * 前置条件：调用方是 `BroadcastReceiver` 且接收的是显式广播。
 * 后置条件：`block` 在 IO 线程执行，执行完成或抛出后调用 `finish()`。
 * 不变式：不使用 `GlobalScope`，异常不会阻止 `finish()`。
 */
internal fun BroadcastReceiver.goAsyncLite(block: suspend () -> Unit) {
    val result = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            block()
        } finally {
            result.finish()
        }
    }
}

/**
 * HITA_Lite 今日组件共用逻辑。
 *
 * 前置条件：`LiteHostBridge.timetable` 由宿主注入；未注入时显示加载占位。
 * 后置条件：RemoteViews 使用 `lite_widget_*` 资源，数据来自宿主 `TimetableService`。
 * 不变式：不创建 Room/DAO/Repository，不读写旧 EAS/登录数据库。
 * 违约处理：宿主服务缺失时返回 `null`，界面显示加载文案。
 */
object LiteTodayUtils {

    const val ACTION_REFRESH = "com.stupidtree.hitax.legacy.lite.WIDGET_REFRESH"
    const val ACTION_REFRESH_COMPAT = "com.stupidtree.hita.WIDGET_EVENT_REFRESH"
    const val ACTION_CLICK = "com.stupidtree.hitax.legacy.lite.WIDGET_CLICK"
    const val EXTRA_EVENT_ID = "com.stupidtree.hitax.legacy.lite.EXTRA_EVENT_ID"
    const val EXTRA_EVENT = "event"
    const val EXTRA_SLIM = "slim"

    fun isWidgetDarkTheme(context: Context): Boolean = when (ThemeTools.getThemeMode(context)) {
        ThemeTools.MODE.DARK -> true
        ThemeTools.MODE.LIGHT -> false
        ThemeTools.MODE.FOLLOW ->
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
    }

    @WorkerThread
    fun todayEvents(): List<EventItem>? = LiteHostBridge.timetable?.getTodayEventsSync()

    fun setUpOneWidget(
        context: Context,
        events: List<EventItem>?,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        slim: Boolean
    ) {
        val views = RemoteViews(
            context.packageName,
            if (slim) R.layout.lite_widget_today_slim else R.layout.lite_widget_today
        )
        val dark = isWidgetDarkTheme(context)
        views.setOnClickPendingIntent(R.id.lite_widget_refresh, refreshPendingIntent(context, appWidgetId, slim))
        views.setOnClickPendingIntent(R.id.lite_widget_refresh_icon, refreshPendingIntent(context, appWidgetId, slim))
        views.setOnClickPendingIntent(R.id.lite_widget_title_area, shellPendingIntent(context, appWidgetId, slim))
        views.setPendingIntentTemplate(
            R.id.lite_widget_list,
            itemPendingIntent(context, appWidgetId, slim)
        )
        applyWidgetTheme(views, context, dark)
        views.setTextViewText(
            R.id.lite_widget_title,
            TimeTools.getDateString(
                context,
                Calendar.getInstance(),
                simplified = true,
                TTYMode = if (slim) TimeTools.TTY_NONE else TimeTools.TTY_WK2_FOLLOWING
            )
        )
        val serviceIntent = Intent(context, LiteListWidgetService::class.java)
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        serviceIntent.putExtra(EXTRA_SLIM, slim)
        serviceIntent.data = Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME))
        views.setRemoteAdapter(R.id.lite_widget_list, serviceIntent)
        val hasEvents = !events.isNullOrEmpty()
        views.setViewVisibility(R.id.lite_widget_list, if (hasEvents) View.VISIBLE else View.GONE)
        views.setViewVisibility(
            R.id.lite_widget_place_holder,
            if (hasEvents) View.GONE else View.VISIBLE
        )
        if (!hasEvents) {
            views.setTextViewText(
                R.id.lite_widget_loading,
                context.getString(
                    if (events == null) R.string.lite_widget_loading else R.string.lite_widget_empty_today
                )
            )
            views.setViewVisibility(R.id.lite_widget_loading_icon, View.VISIBLE)
        }
        views.setBoolean(R.id.lite_widget_refresh, "setEnabled", true)
        appWidgetManager.updateAppWidget(appWidgetId, views)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lite_widget_list)
    }

    fun showRefreshingState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        slim: Boolean
    ) {
        val dark = isWidgetDarkTheme(context)
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(
                context.packageName,
                if (slim) R.layout.lite_widget_today_slim else R.layout.lite_widget_today
            )
            applyWidgetTheme(views, context, dark)
            views.setInt(
                R.id.lite_widget_refresh_icon,
                "setBackgroundResource",
                if (dark) R.drawable.lite_widget_ic_refresh_dark_pressed
                else R.drawable.lite_widget_ic_refresh_pressed
            )
            views.setBoolean(R.id.lite_widget_refresh, "setEnabled", false)
            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
        }
    }

    private fun refreshPendingIntent(context: Context, appWidgetId: Int, slim: Boolean): PendingIntent {
        val intent = Intent(context, if (slim) LiteTodayWidgetSlim::class.java else LiteTodayWidget::class.java)
            .setAction(ACTION_REFRESH)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .setData(Uri.parse("hita://widget/lite/refresh/${if (slim) "slim" else "normal"}/$appWidgetId"))
        return PendingIntent.getBroadcast(
            context,
            appWidgetId * 2 + if (slim) 1 else 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun shellPendingIntent(context: Context, appWidgetId: Int, slim: Boolean): PendingIntent {
        val intent = Intent(context, HitaLiteActivity::class.java)
            .setAction(ACTION_CLICK)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .setData(Uri.parse("hita://widget/lite/open/${if (slim) "slim" else "normal"}/$appWidgetId"))
        return PendingIntent.getActivity(
            context,
            appWidgetId * 2 + if (slim) 1 else 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun itemPendingIntent(context: Context, appWidgetId: Int, slim: Boolean): PendingIntent {
        val intent = Intent(context, HitaLiteActivity::class.java)
            .setAction(ACTION_CLICK)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .setData(Uri.parse("hita://widget/lite/item/${if (slim) "slim" else "normal"}/$appWidgetId"))
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_IMMUTABLE
            }
        return PendingIntent.getActivity(context, appWidgetId * 2 + if (slim) 1 else 0, intent, flags)
    }

    private fun applyWidgetTheme(views: RemoteViews, context: Context, dark: Boolean) {
        views.setInt(
            R.id.lite_widget_container,
            "setBackgroundResource",
            if (dark) R.drawable.lite_widget_today_background_dark
            else R.drawable.lite_widget_today_background
        )
        views.setInt(
            R.id.lite_widget_divider,
            "setBackgroundResource",
            if (dark) R.drawable.lite_widget_rounded_divider_dark
            else R.drawable.lite_widget_rounded_divider
        )
        views.setInt(
            R.id.lite_widget_refresh_icon,
            "setBackgroundResource",
            if (dark) R.drawable.lite_widget_ic_refresh_dark else R.drawable.lite_widget_ic_refresh
        )
        views.setInt(
            R.id.lite_widget_loading_icon,
            "setBackgroundResource",
            if (dark) R.drawable.lite_widget_placeholder_timeline_dark
            else R.drawable.lite_widget_placeholder_timeline
        )
        views.setTextColor(
            R.id.lite_widget_title,
            context.getColor(if (dark) R.color.lite_widget_title_dark else R.color.lite_widget_title_light)
        )
        views.setTextColor(
            R.id.lite_widget_loading,
            context.getColor(if (dark) R.color.lite_widget_empty_dark else R.color.lite_widget_empty_light)
        )
    }
}
