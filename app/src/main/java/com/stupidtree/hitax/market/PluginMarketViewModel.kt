package com.stupidtree.hitax.market

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.stupidtree.hitax.BuildConfig
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.plugin.BuiltinPluginManager
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
    val status = MutableLiveData<String?>(null)
    val apkPlugins = MutableLiveData<List<ApkPlugin>>(emptyList())

    private val builtin = BuiltinPluginManager(application)

    /** 内置插件（可卸载/恢复）。 */
    val builtinPlugins = MutableLiveData<List<BuiltinPluginManager.Row>>(emptyList())

    /** 内置插件变更后需要重建界面（tab / 导航项集合已变）。 */
    val reloadUi = MutableLiveData(false)
    private val apkRepository = ApkPluginRepository(application)
    private val trustStore = PluginTrustStore(application)

    init {
        refresh()
    }

    /** 索引可能在远端，读取放到后台线程。 */
    fun refresh() {
        status.postValue("正在读取插件索引…")
        executor.execute {
            ensureDemoIndex()
            apkPlugins.postValue(
                runCatching { apkRepository.discover() }
                    .onFailure { android.util.Log.e("PluginMarket", "discover apk plugins failed", it) }
                    .getOrDefault(emptyList())
            )
            builtinPlugins.postValue(builtin.rows())
            val installed = installer.installed().map { it.id }.toSet()
            runCatching { readIndex() }
                .onSuccess { (index, _) ->
                    items.postValue(index.plugins.map { Item(it, it.id in installed) })
                    status.postValue(null)
                }
                .onFailure {
                    items.postValue(emptyList())
                    status.postValue("读取索引失败：${it.message}")
                }
        }
    }

    fun supported(plugin: ApkPlugin): Boolean = apkRepository.supported(plugin)

    fun isVerified(id: String): Boolean = trustStore.isVerified(id)

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

    fun uninstallBuiltin(row: BuiltinPluginManager.Row, purgeData: Boolean) {
        executor.execute {
            val error = runCatching { builtin.uninstall(row.id, purgeData) }.exceptionOrNull()
            message.postValue(error?.let { "卸载失败：${it.message}" } ?: "已卸载 ${row.name}")
            builtinPlugins.postValue(builtin.rows())
            if (error == null) reloadUi.postValue(true)
        }
    }

    fun installBuiltin(row: BuiltinPluginManager.Row) {
        executor.execute {
            val error = runCatching { builtin.install(row.id) }.exceptionOrNull()
            message.postValue(error?.let { "启用失败：${it.message}" } ?: "已启用 ${row.name}")
            builtinPlugins.postValue(builtin.rows())
            if (error == null) reloadUi.postValue(true)
        }
    }

    fun consumeReloadUi() {
        reloadUi.value = false
    }

    /** 消息只消费一次，避免旋转屏幕后重复弹。 */
    fun consumeMessage() {
        message.value = null
    }

    private fun post(text: String) {
        message.postValue(text)
        refresh()
    }

    /** @return 索引内容 + 用于解析相对插件地址的基准（远端 URL 或本地目录） */
    private fun readIndex(): Pair<MarketIndex, String?> {
        val ctx = getApplication<Application>()
        return if (BuildConfig.PLUGIN_INDEX_URL.isNotBlank()) {
            val text = java.net.URL(BuildConfig.PLUGIN_INDEX_URL).openStream()
                .bufferedReader().use { it.readText() }
            gson.fromJson(text, MarketIndex::class.java) to BuildConfig.PLUGIN_INDEX_URL
        } else {
            val file = File(ctx.filesDir, "market/index.json")
            gson.fromJson(file.readText(), MarketIndex::class.java) to file.parentFile.absolutePath
        }
    }

    /** 离线演示：未配置远端索引且本地没有索引时，释放内置示例。 */
    private fun ensureDemoIndex() {
        // 演示插件只随 debug 包分发；正式包的市场应有真实索引。
        if (!BuildConfig.DEBUG) return
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
