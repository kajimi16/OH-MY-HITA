package com.stupidtree.hitax.market

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.stupidtree.hita.kernel.PluginHost
import com.stupidtree.hita.kernel.PluginLoader
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * T1 运行时插件的安装器：下载 → 校验 sha256 → 装进私有目录 → DexClassLoader 装载。
 *
 * 不处理 UI，也不决定什么时候安装，只保证「装/卸/恢复」三件事是幂等的。
 */
class PluginInstaller(
    private val context: Context,
    private val host: PluginHost,
) {

    private val root = File(context.filesDir, "runtime-plugins")
    private val installedFile = File(root, "installed.json")
    private val disabledFile = File(root, "disabled.json")
    private val gson = Gson()

    /** 停用集合：保留 plugin.dex，只是不装载。 */
    private fun disabledIds(): Set<String> = runCatching {
        if (!disabledFile.isFile) emptySet()
        else gson.fromJson(disabledFile.readText(), Array<String>::class.java).toSet()
    }.getOrDefault(emptySet())

    private fun saveDisabled(ids: Set<String>) {
        root.mkdirs()
        disabledFile.writeText(gson.toJson(ids.toList()))
    }

    fun isEnabled(id: String): Boolean = id !in disabledIds()

    /** 开关语义：停用只卸载出内核并保留文件；启用从已落盘 dex 重新装载。 */
    fun setEnabled(id: String, enabled: Boolean) {
        val entry = installed().firstOrNull { it.id == id }
            ?: throw IllegalStateException("插件未安装")
        if (enabled) {
            val dir = File(root, id)
            val dex = File(dir, "plugin.dex")
            if (!dex.isFile) throw IllegalStateException("插件文件不存在，请重新安装")
            PluginLoader(host, logger = { m, e -> log(m, e) })
                .loadAndInstall(dex, entry.entryClass, File(dir, "opt"))
            saveDisabled(disabledIds() - id)
        } else {
            runCatching { host.uninstall(id) }
            saveDisabled(disabledIds() + id)
        }
        Log.i(TAG, "setEnabled ${entry.id}=$enabled")
    }

    fun installed(): List<MarketPlugin> = runCatching {
        if (!installedFile.isFile) return emptyList()
        gson.fromJson(installedFile.readText(), Array<MarketPlugin>::class.java).toList()
    }.getOrDefault(emptyList())

    /**
     * 安装一个市场插件。失败时抛异常，调用方负责提示；已产生的文件会被清掉。
     */
    fun install(entry: MarketPlugin, indexBase: String?): Unit {
        val dir = File(root, entry.id)
        dir.deleteRecursively()
        dir.mkdirs()

        try {
            val dex = File(dir, "plugin.dex")
            fetch(entry, indexBase, dex)

            val actual = PluginLoader.sha256(dex)
            if (!actual.equals(entry.sha256, ignoreCase = true)) {
                throw IllegalStateException("sha256 校验失败：期望 ${entry.sha256}，实际 $actual")
            }

            PluginLoader(host, logger = { m, e -> log(m, e) })
                .loadAndInstall(dex, entry.entryClass, File(dir, "opt"))

            saveInstalled(installed().filterNot { it.id == entry.id } + entry)
            saveDisabled(disabledIds() - entry.id)
            Log.i(TAG, "installed ${entry.id}@${entry.version}")
        } catch (t: Throwable) {
            runCatching { host.uninstall(entry.id) }
            dir.deleteRecursively()
            throw t
        }
    }

    fun uninstall(id: String) {
        runCatching { host.uninstall(id) }
        File(root, id).deleteRecursively()
        saveInstalled(installed().filterNot { it.id == id })
        saveDisabled(disabledIds() - id)
    }

    /** 应用启动时恢复上次安装过的运行时插件。单个失败不影响其它插件。 */
    fun restore() {
        installed().forEach { entry ->
            if (!isEnabled(entry.id)) {
                Log.i(TAG, "skip ${entry.id}: disabled")
                return@forEach
            }
            val dex = File(File(root, entry.id), "plugin.dex")
            if (!dex.isFile) {
                Log.w(TAG, "skip ${entry.id}: plugin.dex missing")
                return@forEach
            }
            runCatching {
                PluginLoader(host, logger = { m, e -> log(m, e) })
                    .loadAndInstall(dex, entry.entryClass, File(File(root, entry.id), "opt"))
            }.onFailure { log("restore failed: ${entry.id}", it) }
        }
    }

    private fun fetch(entry: MarketPlugin, indexBase: String?, target: File) {
        val absolute = PluginUrls.resolve(entry.url, indexBase)
        if (absolute.startsWith("http://") || absolute.startsWith("https://")) {
            val conn = URL(absolute).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.inputStream.use { input -> target.outputStream().use { input.copyTo(it) } }
            conn.disconnect()
        } else {
            val source = File(absolute)
            if (!source.isFile) throw IllegalStateException("插件文件不存在：${source.path}")
            source.copyTo(target, overwrite = true)
        }
    }

    private fun saveInstalled(list: List<MarketPlugin>) {
        root.mkdirs()
        installedFile.writeText(gson.toJson(list))
    }

    private fun log(message: String, error: Throwable?) {
        if (error == null) Log.i(TAG, message) else Log.e(TAG, message, error)
    }

    companion object {
        private const val TAG = "PluginInstaller"
    }
}
