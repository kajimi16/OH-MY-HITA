package com.stupidtree.hitax.uiplugin

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.JsonParser
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.HITA_API_VERSION
import dalvik.system.DexClassLoader
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * 运行时界面插件的安装 / 卸载 / 加载。
 *
 * 插件是一个 APK：只有 classes.dex 与 assets/plugin.json，没有四大组件。
 * 宿主把 APK 复制到 filesDir/ui-plugins/<id>/plugin.apk，用 DexClassLoader 加载，
 * 界面由 [com.stupidtree.hitax.uiplugin.PluginUiActivity] 承载。
 */
class UiPluginManager(private val context: Context) {

    private val root = File(context.filesDir, "ui-plugins")

    fun installed(): List<InstalledUiPlugin> =
        UiPluginStore.list(context).filter { File(it.apkPath).isFile }

    data class Metadata(
        val id: String,
        val name: String,
        val version: String,
        val entryClass: String,
        val apiVersion: Int,
    )

    fun install(uri: Uri): Result<InstalledUiPlugin> = runCatching {
        val tmp = File(context.cacheDir, "incoming-plugin.apk")
        context.contentResolver.openInputStream(uri)?.use { input ->
            tmp.outputStream().use { input.copyTo(it) }
        } ?: error("无法读取所选文件")

        val meta = readMetadata(tmp)
        require(meta.apiVersion <= HITA_API_VERSION) {
            "插件需要 api ${meta.apiVersion}，当前宿主 $HITA_API_VERSION"
        }
        val dir = File(root, meta.id).apply { mkdirs() }
        val apk = File(dir, "plugin.apk")
        tmp.copyTo(apk, overwrite = true)
        tmp.delete()
        val digest = sha256(apk)

        val plugin = loadClass(apk, meta.entryClass)
        require(plugin is ComposeUiPlugin) { "入口类未实现 ComposeUiPlugin" }
        require(plugin.id == meta.id) { "id 不一致：${plugin.id} vs ${meta.id}" }
        require(plugin.requiresApi <= HITA_API_VERSION) {
            "插件需要 api ${plugin.requiresApi}，当前宿主 $HITA_API_VERSION"
        }

        val installed = InstalledUiPlugin(
            id = meta.id,
            name = plugin.displayName,
            version = meta.version,
            entryClass = meta.entryClass,
            apkPath = apk.absolutePath,
            sha256 = digest,
        )
        UiPluginStore.put(context, installed)
        installed
    }

    fun uninstall(id: String) {
        UiPluginStore.find(context, id)?.let { plugin ->
            runCatching { File(plugin.apkPath).parentFile?.deleteRecursively() }
                .onFailure { Log.w(TAG, "delete plugin dir failed", it) }
        }
        UiPluginStore.remove(context, id)
    }

    /** 重新加载并校验哈希，然后实例化插件。 */
    fun instantiate(record: InstalledUiPlugin): ComposeUiPlugin {
        val apk = File(record.apkPath)
        check(apk.isFile) { "插件文件丢失" }
        check(sha256(apk) == record.sha256) { "插件文件校验失败（sha256 不一致）" }
        val plugin = loadClass(apk, record.entryClass)
        return plugin as? ComposeUiPlugin ?: error("不是 ComposeUiPlugin")
    }

    private fun loadClass(apk: File, className: String): Any {
        val dexDir = File(apk.parentFile, "oat").apply { mkdirs() }
        val loader = DexClassLoader(
            apk.absolutePath,
            dexDir.absolutePath,
            null,
            context.classLoader,
        )
        val clazz = Class.forName(className, true, loader)
        return clazz.getDeclaredConstructor().newInstance()
    }

    private fun readMetadata(apk: File): Metadata {
        ZipFile(apk).use { zip ->
            val entry = zip.getEntry("assets/plugin.json") ?: error("APK 缺少 assets/plugin.json")
            val text = zip.getInputStream(entry).bufferedReader().use { it.readText() }
            val obj = JsonParser.parseString(text).asJsonObject
            return Metadata(
                id = obj.get("id")?.asString ?: error("metadata.id 缺失"),
                name = obj.get("name")?.asString ?: "",
                version = obj.get("version")?.asString ?: "0",
                entryClass = obj.get("entryClass")?.asString ?: error("metadata.entryClass 缺失"),
                apiVersion = obj.get("apiVersion")?.asInt ?: 1,
            )
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 20)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "UiPluginManager"
    }
}
