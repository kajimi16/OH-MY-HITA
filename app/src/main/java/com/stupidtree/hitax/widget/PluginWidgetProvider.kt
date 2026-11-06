package com.stupidtree.hitax.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.stupidtree.hita.api.ServiceKey
import com.stupidtree.hita.api.WidgetContent
import com.stupidtree.hita.api.WidgetContentProvider
import com.stupidtree.hita.api.WidgetHost
import com.stupidtree.hita.api.WidgetOptions
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.R
import com.stupidtree.hitax.uiplugin.InstalledUiPlugin
import com.stupidtree.hitax.uiplugin.PluginUiActivity
import com.stupidtree.hitax.uiplugin.UiPluginManager

/** 宿主统一的小组件 Provider：插件只提供数据，不注册组件。 */
class PluginWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pending = goAsync()
        Thread {
            try {
                appWidgetIds.forEach { updateOne(context, appWidgetManager, it) }
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {
        private const val PREFS = "plugin_widgets"
        private const val TAG = "PluginWidget"

        fun selectedPlugin(context: Context, appWidgetId: Int): String? =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(key(appWidgetId), null)

        fun selectPlugin(context: Context, appWidgetId: Int, pluginId: String) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(key(appWidgetId), pluginId)
                .apply()
        }

        fun updateWidget(context: Context, appWidgetId: Int) {
            val manager = AppWidgetManager.getInstance(context)
            updateOne(context, manager, appWidgetId)
        }

        private fun key(appWidgetId: Int) = "widget_$appWidgetId"

        private fun updateOne(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_plugin)
            val pluginId = selectedPlugin(context, appWidgetId)
            if (pluginId == null) {
                renderMessage(context, views, "小组件未配置", "点按选择插件")
                manager.updateAppWidget(appWidgetId, views)
                return
            }
            val record = UiPluginManager(context).installed().firstOrNull { it.id == pluginId }
            if (record == null || !record.enabled) {
                renderMessage(context, views, "插件不可用", pluginId)
                manager.updateAppWidget(appWidgetId, views)
                return
            }
            runCatching {
                val plugin = UiPluginManager(context).instantiate(record)
                check(plugin is WidgetContentProvider) { "插件未实现 WidgetContentProvider" }
                val options = manager.getAppWidgetOptions(appWidgetId)
                val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                val content = plugin.buildWidget(context, Host(context), WidgetOptions(width, height))
                render(context, views, appWidgetId, record, content)
                manager.updateAppWidget(appWidgetId, views)
            }.onFailure {
                Log.w(TAG, "render widget failed: $pluginId", it)
                renderMessage(context, views, "组件加载失败", it.message ?: pluginId)
                manager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun render(
            context: Context,
            views: RemoteViews,
            appWidgetId: Int,
            record: InstalledUiPlugin,
            content: WidgetContent,
        ) {
            views.setTextViewText(R.id.widget_title, content.title)
            views.setTextViewText(R.id.widget_subtitle, content.subtitle.orEmpty())
            views.setViewVisibility(
                R.id.widget_subtitle,
                if (content.subtitle.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE,
            )
            views.removeAllViews(R.id.widget_rows)
            content.rows.take(8).forEach { row ->
                val rowView = RemoteViews(context.packageName, R.layout.widget_plugin_row)
                rowView.setTextViewText(R.id.row_leading, row.leading.orEmpty())
                rowView.setTextViewText(R.id.row_title, row.title)
                rowView.setTextViewText(R.id.row_trailing, row.trailing.orEmpty())
                rowView.setViewVisibility(
                    R.id.row_leading,
                    if (row.leading.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE,
                )
                rowView.setViewVisibility(
                    R.id.row_trailing,
                    if (row.trailing.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE,
                )
                views.addView(R.id.widget_rows, rowView)
            }
            val intent = Intent(context, PluginUiActivity::class.java)
                .putExtra(PluginUiActivity.EXTRA_PLUGIN_ID, record.id)
            val pending = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
        }

        private fun renderMessage(
            context: Context,
            views: RemoteViews,
            title: String,
            subtitle: String,
        ) {
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_subtitle, subtitle)
            views.setViewVisibility(R.id.widget_subtitle, android.view.View.VISIBLE)
            views.removeAllViews(R.id.widget_rows)
        }
    }

    private class Host(private val context: Context) : WidgetHost {
        override fun <T : Any> service(key: ServiceKey<T>): T? =
            (context.applicationContext as HApplication).pluginHost.service(key)
    }
}
