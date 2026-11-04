package com.stupidtree.hitax.uiplugin

import android.content.Context
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
        UiPluginStore.list(context)
            .filter { File(it.apkPath).isFile }
            .map { it.copy(enabled = !UiPluginStore.isDisabled(context, it.id)) }

    /** 按 capability 找一个可用的已安装插件；找不到返回 null。 */
    fun enabled(): List<InstalledUiPlugin> = installed().filter { it.enabled }

    fun findByCapability(capability: String): InstalledUiPlugin? =
        enabled().firstOrNull { capability in it.capabilities }

    /** 启用/停用已安装插件；停用后不再参与能力路由，但文件保留。 */
    fun setEnabled(id: String, enabled: Boolean): Boolean {
        UiPluginStore.find(context, id) ?: return false
        UiPluginStore.setEnabled(context, id, enabled)
        return true
    }

    data class Metadata(
        val id: String,
        val name: String,
        val version: String,
        val entryClass: String,
        val apiVersion: Int,
        val composeVersion: String,
        val capabilities: List<String>,
    )

    private fun requireValidId(id: String) {
        require(ID_PATTERN.matches(id)) { "非法的插件 id：$id" }
    }

    /** 插件目录必须严格位于 root 之下，防止 id 里带 ../ 越界。 */
    private fun pluginDir(id: String): File {
        requireValidId(id)
        val rootCanonical = root.canonicalFile
        val dir = File(rootCanonical, id).canonicalFile
        require(dir.parentFile == rootCanonical) { "插件目录越界：$id" }
        return dir
    }


    /**
     * 从已落盘的文件安装；[expectedSha256] 来自市场索引，下载后先比对再落库。
     */
    fun installFile(file: File, expectedSha256: String?): InstalledUiPlugin {
        val actual = sha256(file)
        if (expectedSha256 != null) {
            require(actual.equals(expectedSha256, ignoreCase = true)) {
                "sha256 校验失败：期望 $expectedSha256，实际 $actual"
            }
        }
        val meta = readMetadata(file)
        require(meta.apiVersion <= HITA_API_VERSION) {
            "插件需要 api ${meta.apiVersion}，当前宿主 $HITA_API_VERSION"
        }
        require(meta.composeVersion.isBlank() || meta.composeVersion == COMPOSE_RUNTIME_VERSION) {
            "插件基于 Compose ${meta.composeVersion}，宿主运行时为 $COMPOSE_RUNTIME_VERSION"
        }
        val dir = pluginDir(meta.id).apply { mkdirs() }
        // 先落到 staging 并通过校验，再替换旧版本，避免坏包把已装插件弄坏
        val staging = File(dir, "plugin-new.apk")
        file.copyTo(staging, overwrite = true)
        require(sha256(staging) == actual) { "复制后校验失败" }

        val plugin = loadClass(staging, meta.entryClass)
        require(plugin is ComposeUiPlugin) { "入口类未实现 ComposeUiPlugin" }
        require(plugin.id == meta.id) { "id 不一致：${plugin.id} vs ${meta.id}" }
        require(plugin.requiresApi <= HITA_API_VERSION) {
            "插件需要 api ${plugin.requiresApi}，当前宿主 $HITA_API_VERSION"
        }

        // 替换旧 APK 并清掉旧优化产物，避免 DexClassLoader 复用旧 dex
        File(dir, "plugin.apk").delete()
        File(dir, "oat").deleteRecursively()
        val apk = File(dir, "plugin.apk")
        check(staging.renameTo(apk)) { "替换插件文件失败" }
        // Android 14 起，动态加载的代码文件必须是只读的
        apk.setReadOnly()
        require(sha256(apk) == actual) { "落盘校验失败" }

        val installed = InstalledUiPlugin(
            id = meta.id,
            name = plugin.displayName,
            version = meta.version,
            entryClass = meta.entryClass,
            apkPath = apk.absolutePath,
            sha256 = actual,
            capabilities = meta.capabilities,
        )
        UiPluginStore.put(context, installed)
        return installed
    }

    fun uninstall(id: String) {
        val dir = runCatching { pluginDir(id) }.getOrNull()
        if (dir != null) {
            runCatching { dir.deleteRecursively() }
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
                composeVersion = obj.get("composeVersion")?.asString ?: "",
                capabilities = obj.getAsJsonArray("capabilities")
                    ?.mapNotNull { it.asString } ?: emptyList(),
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
        private const val COMPOSE_RUNTIME_VERSION = com.stupidtree.hitax.BuildConfig.COMPOSE_RUNTIME_VERSION
        private val ID_PATTERN = Regex("^[a-zA-Z0-9][a-zA-Z0-9._-]{0,63}$")
    }
}
