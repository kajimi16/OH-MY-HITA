package com.example.hitaplugin

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

/**
 * T2 示例：一个拥有自己界面的独立插件。
 *
 * 宿主通过 `com.stupidtree.hita.action.PLUGIN_APK` 发现这个 Activity，
 * 通过 `<meta-data>` 读取 id 与契约版本，然后直接启动它。
 */
class SamplePluginActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 96, 64, 64)
        }
        root.addView(TextView(this).apply {
            text = "独立 APK 插件"
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = "这个页面完全属于插件自己的应用，宿主只负责发现并启动它。\n\n"
            textSize = 15f
        })
        root.addView(TextView(this).apply {
            text = "package: $packageName\napiVersion: 2"
            textSize = 13f
        })
        setContentView(root)
    }
}
