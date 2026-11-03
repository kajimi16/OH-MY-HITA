package com.stupidtree.hitax.market

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.stupidtree.hitax.BuildConfig
import com.stupidtree.hitax.HApplication
import java.io.File
import java.util.concurrent.Executors

/**
 * 插件市场的状态与操作。Fragment 只负责渲染。
 */
class PluginMarketViewModel(application: Application) : AndroidViewModel(application) {

    data class Item(val entry: MarketPlugin, val installed: Boolean)

    private val installer = PluginInstaller(application, (application as HApplication).pluginHost)
    private val executor = Executors.newSingleThreadExecutor()
    private val gson = Gson()

    val items = MutableLiveData<List<Item>>(emptyList())
    val message = MutableLiveData<String?>(null)

    init {
        ensureDemoIndex()
        refresh()
    }

    fun refresh() {
        val ctx = getApplication<Application>()
        val installed = installer.installed().map { it.id }.toSet()
        runCatching { readIndex() }
            .onSuccess { (index, _) ->
                items.postValue(index.plugins.map { Item(it, it.id in installed) })
            }
            .onFailure {
                items.postValue(emptyList())
                message.postValue("读取索引失败：${it.message}")
            }
    }

    fun install(entry: MarketPlugin) {
        executor.execute {
            val base = runCatching { readIndex().second }.getOrNull()
            val error = runCatching { installer.install(entry, base) }.exceptionOrNull()
            post(error?.let { "安装失败：${it.message}" } ?: "已安装 ${entry.name}")
        }
    }

    fun uninstall(entry: MarketPlugin) {
        executor.execute {
            val error = runCatching { installer.uninstall(entry.id) }.exceptionOrNull()
            post(error?.let { "卸载失败：${it.message}" } ?: "已卸载 ${entry.name}")
        }
    }

    /** 消息只消费一次，避免旋转屏幕后重复弹。 */
    fun consumeMessage() {
        message.value = null
    }

    private fun post(text: String) {
        message.postValue(text)
        refresh()
    }

    private fun readIndex(): Pair<MarketIndex, File?> {
        val ctx = getApplication<Application>()
        return if (BuildConfig.PLUGIN_INDEX_URL.isNotBlank()) {
            val text = java.net.URL(BuildConfig.PLUGIN_INDEX_URL).openStream().bufferedReader().use { it.readText() }
            gson.fromJson(text, MarketIndex::class.java) to null
        } else {
            val file = File(ctx.filesDir, "market/index.json")
            gson.fromJson(file.readText(), MarketIndex::class.java) to file.parentFile
        }
    }

    /** 离线演示：未配置远端索引且本地没有索引时，释放内置示例。 */
    private fun ensureDemoIndex() {
        if (BuildConfig.PLUGIN_INDEX_URL.isNotBlank()) return
        val ctx = getApplication<Application>()
        val dir = File(ctx.filesDir, "market")
        if (File(dir, "index.json").isFile) return
        dir.mkdirs()
        runCatching {
            ctx.assets.open("market/index.json").use { input ->
                File(dir, "index.json").outputStream().use { input.copyTo(it) }
            }
            ctx.assets.open("market/plugin.dex").use { input ->
                File(dir, "plugin.dex").outputStream().use { input.copyTo(it) }
            }
        }
    }
}
