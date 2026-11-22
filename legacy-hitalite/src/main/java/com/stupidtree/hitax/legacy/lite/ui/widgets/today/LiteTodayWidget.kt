package com.stupidtree.hitax.legacy.lite.ui.widgets.today

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * HITA_Lite 今日组件（标准尺寸）。
 *
 * 前置条件：系统或显式广播携带 `APPWIDGET_UPDATE`/刷新动作。
 * 后置条件：名下所有实例按宿主课表刷新；宿主服务缺失时显示加载占位。
 * 不变式：只读 `LiteHostBridge`，不创建数据库、Repository 或登录单例。
 */
class LiteTodayWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        goAsyncLite {
            val events = LiteTodayUtils.todayEvents()
            for (appWidgetId in appWidgetIds) {
                LiteTodayUtils.setUpOneWidget(context, events, appWidgetManager, appWidgetId, false)
            }
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        when (intent?.action) {
            LiteTodayUtils.ACTION_REFRESH,
            LiteTodayUtils.ACTION_REFRESH_COMPAT -> refresh(context)
        }
    }

    private fun refresh(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val appWidgetIds = manager.getAppWidgetIds(ComponentName(context, LiteTodayWidget::class.java))
        LiteTodayUtils.showRefreshingState(context, manager, appWidgetIds, slim = false)
        goAsyncLite {
            val events = LiteTodayUtils.todayEvents()
            for (appWidgetId in appWidgetIds) {
                LiteTodayUtils.setUpOneWidget(context, events, manager, appWidgetId, false)
            }
        }
    }
}
