package com.stupidtree.hitax.market

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.stupidtree.hitax.BuildConfig
import com.stupidtree.hitax.HApplication
import java.io.File
import java.util.concurrent.Executors

/**
 * 插件市场（骨架）。
 *
 * 索引来源：`PLUGIN_INDEX_URL`（构建期注入），为空时读应用私有目录下的
 * `market/index.json`，便于本地联调。
 *
 * 只列「逻辑插件」：它们提供数据源、同步表、算法等，不携带界面与资源。
 */
class PluginMarketActivity : Activity() {

    private lateinit var installer: PluginInstaller
    private lateinit var list: LinearLayout
    private val executor = Executors.newSingleThreadExecutor()
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installer = PluginInstaller(applicationContext, (application as HApplication).pluginHost)
        setContentView(buildContentView())
        ensureDemoIndex()
        refresh()
    }

    private fun buildContentView(): ScrollView {
        val scroll = ScrollView(this)
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        scroll.addView(list)
        return scroll
    }

    /**
     * 离线演示：首次打开市场时，把内置的示例索引与 dex 释放到私有目录。
     * 配置了 PLUGIN_INDEX_URL 时不会走到这里。
     */
    private fun ensureDemoIndex() {
        if (BuildConfig.PLUGIN_INDEX_URL.isNotBlank()) return
        val dir = File(filesDir, "market")
        if (File(dir, "index.json").isFile) return
        dir.mkdirs()
        runCatching {
            assets.open("market/index.json").use { input ->
                File(dir, "index.json").outputStream().use { input.copyTo(it) }
            }
            assets.open("market/plugin.dex").use { input ->
                File(dir, "plugin.dex").outputStream().use { input.copyTo(it) }
            }
        }
    }

    private fun refresh() {
        list.removeAllViews()
        list.addView(title("插件市场"))
        list.addView(hint("索引：${indexLocation()}"))

        val index = runCatching { readIndex() }.getOrElse {
            list.addView(hint("读取索引失败：${it.message}"))
            null
        } ?: return

        val installed = installer.installed().map { it.id }.toSet()

        if (index.first.plugins.isEmpty()) {
            list.addView(hint("索引里没有插件。"))
        }

        index.first.plugins.forEach { entry ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 24, 0, 24)
            }
            row.addView(TextView(this).apply {
                text = "${entry.name}  ${entry.version}  (api ${entry.apiVersion})"
                textSize = 18f
            })
            row.addView(TextView(this).apply {
                text = entry.id
                textSize = 12f
            })
            row.addView(Button(this).apply {
                text = if (entry.id in installed) "卸载" else "安装"
                setOnClickListener {
                    if (entry.id in installed) {
                        installer.uninstall(entry.id)
                        toast("已卸载 ${entry.name}")
                        refresh()
                    } else {
                        installAsync(entry, index.second)
                    }
                }
            })
            list.addView(row)
        }

        list.addView(Button(this).apply {
            text = "重新读取索引"
            setOnClickListener { refresh() }
        })
    }

    private fun installAsync(entry: MarketPlugin, base: File?) {
        toast("正在安装 ${entry.name}…")
        executor.execute {
            val error = runCatching { installer.install(entry, base) }.exceptionOrNull()
            runOnUiThread {
                if (error == null) toast("已安装 ${entry.name}") else toast("安装失败：${error.message}")
                refresh()
            }
        }
    }

    private fun readIndex(): Pair<MarketIndex, File?> {
        val configured = BuildConfig.PLUGIN_INDEX_URL
        return if (configured.isNotBlank()) {
            val text = java.net.URL(configured).openStream().bufferedReader().use { it.readText() }
            gson.fromJson(text, MarketIndex::class.java) to null
        } else {
            val file = File(filesDir, "market/index.json")
            gson.fromJson(file.readText(), MarketIndex::class.java) to file.parentFile
        }
    }

    private fun indexLocation(): String =
        BuildConfig.PLUGIN_INDEX_URL.ifBlank { File(filesDir, "market/index.json").path }

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        textSize = 24f
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(0, 0, 0, 24)
    }

    private fun hint(text: String) = TextView(this).apply {
        this.text = text
        textSize = 12f
        setPadding(0, 0, 0, 16)
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
