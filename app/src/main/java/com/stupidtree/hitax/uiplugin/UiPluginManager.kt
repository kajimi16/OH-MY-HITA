package com.stupidtree.hitax.uiplugin

import android.content.Context
import android.util.Log
import com.google.gson.JsonParser
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.HITA_API_VERSION
import com.stupidtree.hita.kernel.CompatibilityResult
import com.stupidtree.hita.kernel.PluginArtifactMeta
import com.stupidtree.hita.kernel.PluginCompatibility
import com.stupidtree.hitax.BuildConfig
import com.stupidtree.hitax.market.CompositionKind
import com.stupidtree.hitax.market.CompositionStore
import com.stupidtree.hitax.market.CompositionTarget
import com.stupidtree.hitax.plugin.PluginExemptions
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
            .filter { isUsable(it) }
            .map { it.copy(enabled = !UiPluginStore.isDisabled(context, it.id)) }

    data class InstalledMainTab(
        val plugin: InstalledUiPlugin,
        val spec: com.stupidtree.hita.api.MainTabSpec,
    )

    /** 所有启用插件贡献的主界面 tab，按 order 排序。 */
    fun mainTabs(): List<InstalledMainTab> = enabled()
        .flatMap { plugin ->
            val tabs = plugin.mainTabs ?: if (
                com.stupidtree.hita.api.RuntimeUiService.CAP_MAIN_TAB in plugin.capabilities
            ) {
                listOf(
                    com.stupidtree.hita.api.MainTabSpec(
                        id = plugin.id,
                        title = plugin.name,
                    )
                )
            } else {
                emptyList()
            }
            tabs.map { InstalledMainTab(plugin, it) }
        }
        .sortedBy { it.spec.order }

    /** 从插件 APK 的 assets 中读取主界面 tab 图标；失败回退 null。 */
    fun loadMainTabIcon(record: InstalledUiPlugin, assetPath: String?): android.graphics.drawable.Drawable? {
        val path = assetPath?.trim()?.trimStart('/')?.takeIf { it.isNotBlank() } ?: return null
        if (path.contains("..")) return null
        return runCatching {
            java.util.zip.ZipFile(record.apkPath).use { zip ->
                val entry = zip.getEntry("assets/$path") ?: return@use null
                val bitmap = android.graphics.BitmapFactory.decodeStream(zip.getInputStream(entry))
                    ?: return@use null
                android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
            }
        }.getOrNull()
    }

    /** 按 capability 找一个可用的已安装插件；找不到返回 null。 */
    fun enabled(): List<InstalledUiPlugin> {
        val safety = CompositionStore.killSwitches(context)
        return installed().filter {
            it.enabled && CompositionTarget(CompositionKind.UI, it.id) !in safety
        }
    }

    /**
     * 路由前预检：文件必须存在且哈希与安装记录一致。
     *
     * 损坏的插件直接停用并返回 null，让调用方回退内置页面，而不是等
     * [com.stupidtree.hitax.uiplugin.PluginUiActivity] 打开后才失败。
     */
    fun resolveForCapability(capability: String): InstalledUiPlugin? =
        enabled().firstOrNull { capability in it.capabilities }

    fun isUsable(record: InstalledUiPlugin): Boolean = runCatching {
        val apk = File(record.apkPath)
        apk.isFile && sha256(apk).equals(record.sha256, ignoreCase = true)
    }.getOrDefault(false)

    /** 启用/停用已安装插件；停用后不再参与能力路由，但文件保留。 */
    fun setEnabled(id: String, enabled: Boolean): Boolean {
        UiPluginStore.find(context, id) ?: return false
        if (enabled && CompositionTarget(CompositionKind.UI, id) in CompositionStore.killSwitches(context)) {
            return false
        }
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
        val mainTabs: List<com.stupidtree.hita.api.MainTabSpec> = emptyList(),
        val minSdk: Int = 0,
        val minHostApi: Int? = null,
        val maxHostApi: Int? = null,
        val apiAbi: String? = null,
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
     * 从已准备好的文件安装；调用方负责下载/落 staging，[expectedSha256] 来自市场索引。
     *
     * 这里仍会重新计算哈希并和 [expectedSha256] 比对，防止准备与落盘之间文件被替换。
     */
    fun installFile(file: File, expectedSha256: String?): InstalledUiPlugin {
        val actual = sha256(file)
        if (expectedSha256 != null) {
            require(actual.equals(expectedSha256, ignoreCase = true)) {
                "sha256 校验失败：期望 $expectedSha256，实际 $actual"
            }
        }
        val meta = readMetadata(file)
        require(!CompositionStore.isKillSwitched(context, CompositionKind.UI, meta.id)) {
            "插件已被宿主安全开关停用"
        }
        val compatibility = PluginCompatibility.check(
            meta = PluginArtifactMeta(
                id = meta.id,
                requiresApi = meta.apiVersion,
                minHostApi = meta.minHostApi,
                maxHostApi = meta.maxHostApi,
                apiAbi = meta.apiAbi,
                minSdk = meta.minSdk,
            ),
            hostApi = HITA_API_VERSION,
            hostAbi = BuildConfig.API_ABI.takeIf { it.isNotBlank() },
            sdkInt = android.os.Build.VERSION.SDK_INT,
            exempt = PluginExemptions.isExempt(
                context,
                meta.id,
                meta.version,
                HITA_API_VERSION,
            ),
        )
        if (compatibility is CompatibilityResult.Rejected) {
            error("插件不兼容：${compatibility.reasons.joinToString("; ")}")
        }
        require(meta.composeVersion.isBlank() || meta.composeVersion == COMPOSE_RUNTIME_VERSION) {
            "插件基于 Compose ${meta.composeVersion}，宿主运行时为 $COMPOSE_RUNTIME_VERSION"
        }
        val dir = pluginDir(meta.id).apply { mkdirs() }
        // 先落到 staging 并通过校验，再替换旧版本，避免坏包把已装插件弄坏
        val staging = File(dir, "plugin-new.apk")
        file.copyTo(staging, overwrite = true)
        require(sha256(staging) == actual) { "复制后校验失败" }
        // Android 14+ 要求动态加载的 dex/apk 不可写；在类加载前就收紧权限。
        staging.setReadOnly()

        // 替换前保留旧 APK；后续任一步失败都恢复旧文件与旧记录，避免“新版本没装成、旧版本也没了”。
        val apk = File(dir, "plugin.apk")
        val backup = File(dir, "plugin-old.apk")
        val oldRecord = UiPluginStore.find(context, meta.id)
        backup.delete()
        if (apk.isFile) check(apk.renameTo(backup)) { "无法备份旧插件" }
        try {
            File(dir, "oat").deleteRecursively()
            check(staging.renameTo(apk)) { "替换插件文件失败" }
            // Android 14 起，动态加载的代码文件必须是只读的
            apk.setReadOnly()
            require(sha256(apk) == actual) { "落盘校验失败" }
            // 必须从最终路径加载：staging 会被 rename，旧 DexClassLoader 的路径随后失效。
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
                sha256 = actual,
                capabilities = meta.capabilities,
                mainTabs = meta.mainTabs.ifEmpty {
                    if (com.stupidtree.hita.api.RuntimeUiService.CAP_MAIN_TAB in meta.capabilities) {
                        listOf(
                            com.stupidtree.hita.api.MainTabSpec(
                                id = meta.id,
                                title = plugin.displayName,
                            )
                        )
                    } else {
                        emptyList()
                    }
                },
            )
            UiPluginStore.put(context, installed)
            // 重装同一个 id 时旧记录可能仍在 disabled 集合里；安装完成应视为启用。
            UiPluginStore.setEnabled(context, meta.id, true)
            backup.delete()
            com.stupidtree.hitax.analytics.UsageAnalytics.record(
                context,
                com.stupidtree.hitax.analytics.UsageEventAllowlist.PLUGIN_INSTALL,
                pluginId = meta.id,
            )
            return installed
        } catch (t: Throwable) {
            staging.delete()
            apk.delete()
            if (backup.isFile) backup.renameTo(apk)
            if (oldRecord != null) UiPluginStore.put(context, oldRecord) else UiPluginStore.remove(context, meta.id)
            throw t
        }
    }

    /** 供 materializer 在准备阶段校验已下载的文件；安装阶段还会再校验一次。 */
    fun sha256Of(file: File): String = sha256(file)

    fun uninstall(id: String) {
        runCatching { com.stupidtree.hitax.background.BackgroundTaskServiceImpl(context).cancelAll(id) }
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
                minSdk = obj.get("minSdk")?.asInt ?: 0,
                minHostApi = obj.get("minHostApi")?.asInt,
                maxHostApi = obj.get("maxHostApi")?.asInt,
                apiAbi = obj.get("apiAbi")?.asString,
                mainTabs = obj.getAsJsonArray("mainTabs")
                    ?.mapNotNull { element ->
                        runCatching {
                            val tab = element.asJsonObject
                            com.stupidtree.hita.api.MainTabSpec(
                                id = tab.get("id")?.asString ?: return@runCatching null,
                                title = tab.get("title")?.asString ?: "",
                                order = tab.get("order")?.asInt ?: 100,
                                capability = tab.get("capability")?.asString
                                    ?: com.stupidtree.hita.api.RuntimeUiService.CAP_MAIN_TAB,
                                iconAsset = tab.get("iconAsset")?.asString,
                            )
                        }.getOrNull()
                    }.orEmpty(),
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
