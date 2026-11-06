package com.stupidtree.hitax.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.WidgetContentProvider
import com.stupidtree.hitax.uiplugin.UiPluginManager
import java.util.concurrent.Executors

/** 选择哪个已安装的 UI 插件来填充宿主小组件。 */
class PluginWidgetConfigureActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var list: ListView
    private lateinit var empty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish(); return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 24)
        }
        root.addView(TextView(this).apply {
            text = "选择小组件插件"
            textSize = 20f
        })
        empty = TextView(this).apply {
            text = "没有支持小组件的已安装插件"
            visibility = View.GONE
        }
        root.addView(empty)
        list = ListView(this)
        root.addView(list, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f,
        ))
        root.addView(Button(this).apply {
            text = "取消"
            setOnClickListener {
                setResult(RESULT_CANCELED)
                finish()
            }
        })
        setContentView(root)

        executor.execute {
            val providers = UiPluginManager(this).installed()
                .filter { it.enabled }
                .mapNotNull { record ->
                    runCatching {
                        val plugin: ComposeUiPlugin = UiPluginManager(this).instantiate(record)
                        if (plugin is WidgetContentProvider) record to plugin else null
                    }.getOrNull()
                }
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                empty.visibility = if (providers.isEmpty()) View.VISIBLE else View.GONE
                list.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_list_item_1,
                    providers.map { it.first.name },
                )
                list.setOnItemClickListener { _, _, position, _ ->
                    val record = providers[position].first
                    PluginWidgetProvider.selectPlugin(this, appWidgetId, record.id)
                    PluginWidgetProvider.updateWidget(this, appWidgetId)
                    setResult(
                        RESULT_OK,
                        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                    )
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
