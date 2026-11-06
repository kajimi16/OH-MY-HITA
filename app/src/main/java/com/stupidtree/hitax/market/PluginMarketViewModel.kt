package com.stupidtree.hitax.market

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.stupidtree.hita.api.HITA_API_VERSION
import com.stupidtree.hitax.BuildConfig
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.plugin.BuiltinPluginManager
import com.stupidtree.hitax.plugin.PluginPrefs
import com.stupidtree.hitax.uiplugin.InstalledUiPlugin
import com.stupidtree.hitax.uiplugin.UiPluginManager
import java.io.File
import java.util.concurrent.Executors

/**
 * 插件市场的状态与操作。Fragment 只负责渲染。
 */
class PluginMarketViewModel(application: Application) : AndroidViewModel(application) {

    data class Item(
        val entry: MarketPlugin,
        val installed: Boolean,
        val enabled: Boolean,
        val update: Boolean = false,
    )

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

    private val uiPluginManager = UiPluginManager(application)

    /** 运行时界面插件（Compose 容器，独立 APK）。 */
    val uiPlugins = MutableLiveData<List<InstalledUiPlugin>>(emptyList())

    /** 索引里可安装 / 可更新的界面插件。 */
    val availableUiPlugins = MutableLiveData<List<UiMarketPlugin>>(emptyList())

    /** 索引里的整合包（可选字段）。 */
    val presets = MutableLiveData<List<MarketPreset>>(emptyList())

    /** 当前生效的整合包 id；切换成功后更新。 */
    val activePresetId = MutableLiveData<String?>(PresetStateStore.activeId(application))

    /** 索引是否已完成一次读取（成功或失败），用于区分“还在加载”和“没有预设”。 */
    val presetsLoaded = MutableLiveData(false)

    /** 可选插件源（默认源 + 自定义源）。 */
    val sources = MutableLiveData<List<MarketSource>>(emptyList())

    /** 其它来源（非当前源）里可更新的插件数量。 */
    val pendingUpdates = MutableLiveData(0)
    private val apkRepository = ApkPluginRepository(application)
    private val trustStore = PluginTrustStore(application)

    /** 当前索引位置（远端 URL 或本地目录），供相对地址解析。 */
    @Volatile
    private var lastIndexBase: String? = null

    fun indexBase(): String? = lastIndexBase

    /** 从持久化同步当前整合包；首启在别的 ViewModel 应用后，市场需要重新读取。 */
    fun syncActivePreset() {
        activePresetId.value = PresetStateStore.activeId(ctx())
    }

    private fun cacheDirFor(url: String): File {
        val name = java.security.MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
        return File(cacheDir, name)
    }

    private val cacheDir: File = File(application.filesDir, "market-cache")

    init {
        refresh()
    }

    /** 索引可能在远端，读取放到后台线程。 */
    fun refresh() {
        status.postValue("正在读取插件索引…")
        presetsLoaded.postValue(false)
        executor.execute {
            ensureDemoIndex()
            apkPlugins.postValue(
                runCatching { apkRepository.discover() }
                    .onFailure { android.util.Log.e("PluginMarket", "discover apk plugins failed", it) }
                    .getOrDefault(emptyList())
            )
            builtinPlugins.postValue(builtin.rows())
            sources.postValue(MarketSources.all(ctx()))
            uiPlugins.postValue(uiPluginManager.installed())
            availableUiPlugins.postValue(emptyList())
            val installedList = installer.installed()
            val installed = installedList.map { it.id }.toSet()
            val enabled = installedList.filter { installer.isEnabled(it.id) }.map { it.id }.toSet()
            val installedVersions = installedList.associate { it.id to it.version }
            runCatching { readIndex() }
                .onSuccess { (index, base, offline) ->
                    lastIndexBase = base
                    items.postValue(
                        index.plugins.map {
                            Item(
                                it,
                                it.id in installed,
                                it.id in enabled,
                                it.id in installedVersions &&
                                    PluginVersion.isNewer(it.version, installedVersions.getValue(it.id)),
                            )
                        }
                    )
                    availableUiPlugins.postValue(index.uiPlugins)
                    presets.postValue(index.presets.orEmpty())
                    presetsLoaded.postValue(true)
                    status.postValue(if (offline) "网络不可用，正在显示缓存索引" else null)
                    pendingUpdates.postValue(countUpdatesOutsideActive())
                }
                .onFailure {
                    items.postValue(emptyList())
                    availableUiPlugins.postValue(emptyList())
                    presets.postValue(emptyList())
                    presetsLoaded.postValue(true)
                    status.postValue("读取索引失败：${it.message}")
                }
        }
    }

    /** 统计安装来源不是当前源、但在原来源里有新版本的插件数量。 */
    private fun countUpdatesOutsideActive(): Int {
        val active = MarketSources.active(ctx())
        val ui = uiPluginManager.installed()
        val dex = installer.installed()
        val sources = (ui.map { it.id } + dex.map { it.id })
            .map { id -> sourceFor(id) }
            .filter { it.url.isNotBlank() && it.url != active.url }
            .distinctBy { it.url }
        var count = 0
        sources.forEach { source ->
            val index = runCatching { readIndex(source) }.getOrNull() ?: return@forEach
            ui.filter { sourceFor(it.id).url == source.url }.forEach { installed ->
                val entry = index.first.uiPlugins.firstOrNull { it.id == installed.id }
                if (entry != null && PluginVersion.isNewer(entry.version, installed.version)) count++
            }
            dex.filter { sourceFor(it.id).url == source.url }.forEach { installed ->
                val entry = index.first.plugins.firstOrNull { it.id == installed.id }
                if (entry != null && PluginVersion.isNewer(entry.version, installed.version)) count++
            }
        }
        return count
    }

    private fun sourceFor(id: String): MarketSource {
        val recorded = MarketSources.installSource(ctx(), id)
        return if (recorded.isNullOrBlank()) MarketSources.active(ctx()) else MarketSource("recorded", recorded)
    }

    fun activeSource(): MarketSource = MarketSources.active(ctx())

    fun selectSource(source: MarketSource) {
        MarketSources.setActive(ctx(), source)
        refresh()
    }

    fun addSource(name: String, url: String): String? {
        val error = MarketSources.add(ctx(), name, url)
        if (error == null) {
            MarketSources.all(ctx()).lastOrNull { it.url == url.trim() }
                ?.let { MarketSources.setActive(ctx(), it) }
            refresh()
        }
        return error
    }

    fun removeSource(source: MarketSource) {
        MarketSources.remove(ctx(), source)
        refresh()
    }

    private fun ctx(): Application = getApplication()

    fun supported(plugin: ApkPlugin): Boolean = apkRepository.supported(plugin)

    fun isVerified(id: String): Boolean = trustStore.isVerified(id)

    fun install(entry: MarketPlugin) {
        executor.execute {
            val beforeNav = navIds()
            val error = runCatching { installDexBlocking(entry) }.exceptionOrNull()
            if (error == null && navIds() != beforeNav) reloadUi.postValue(true)
            post(error?.let { "安装失败：${it.message}" } ?: "已安装 ${entry.name}")
        }
    }

    private fun installDexBlocking(entry: MarketPlugin) {
        val minApi = entry.minHostApi ?: 1
        require(minApi <= HITA_API_VERSION) {
            "插件需要宿主 API $minApi，当前 ${HITA_API_VERSION}"
        }
        installer.install(entry, lastIndexBase)
        MarketSources.recordInstall(ctx(), entry.id, activeSource().url)
    }

    fun uninstall(entry: MarketPlugin) {
        executor.execute {
            val beforeNav = navIds()
            val error = runCatching { installer.uninstall(entry.id) }.exceptionOrNull()
            if (error == null && navIds() != beforeNav) reloadUi.postValue(true)
            post(error?.let { "卸载失败：${it.message}" } ?: "已卸载 ${entry.name}")
        }
    }

    fun uninstallBuiltin(row: BuiltinPluginManager.Row, purgeData: Boolean) {
        executor.execute {
            val error = runCatching { builtin.uninstall(row.id, purgeData) }.exceptionOrNull()
            message.postValue(error?.let { "卸载失败：${it.message}" } ?: "已卸载 ${row.name}")
            builtinPlugins.postValue(builtin.rows())
            sources.postValue(MarketSources.all(ctx()))
            uiPlugins.postValue(uiPluginManager.installed())
            if (error == null) reloadUi.postValue(true)
        }
    }

    /** 开关语义：启用=安装，停用=卸载但保留数据；变更后重建界面。 */
    fun setBuiltinEnabled(row: BuiltinPluginManager.Row, enabled: Boolean) {
        executor.execute {
            val error = runCatching { builtin.setEnabled(row.id, enabled) }.exceptionOrNull()
            message.postValue(error?.let { "操作失败：${it.message}" } ?: null)
            builtinPlugins.postValue(builtin.rows())
            if (error == null) reloadUi.postValue(true)
        }
    }

    fun setUiPluginEnabled(plugin: InstalledUiPlugin, enabled: Boolean) {
        executor.execute {
            uiPluginManager.setEnabled(plugin.id, enabled)
            uiPlugins.postValue(uiPluginManager.installed())
            if (hasMainTab(plugin)) reloadUi.postValue(true)
        }
    }

    private fun hasMainTab(plugin: InstalledUiPlugin): Boolean =
        com.stupidtree.hita.api.RuntimeUiService.CAP_MAIN_TAB in plugin.capabilities ||
            !plugin.mainTabs.isNullOrEmpty()

    private fun navIds(): Set<String> =
        (ctx() as HApplication).pluginHost.nav.entries().map { it.id }.toSet()

    /** 停用保留已下载文件，只从内核卸载；启用重新装载。 */
    fun setMarketEnabled(entry: MarketPlugin, enabled: Boolean) {
        executor.execute {
            val error = runCatching { installer.setEnabled(entry.id, enabled) }.exceptionOrNull()
            message.postValue(error?.let { "操作失败：${it.message}" } ?: null)
            refresh()
        }
    }

    fun setApkEnabled(packageName: String, enabled: Boolean) {
        PluginPrefs.setDisabled(getApplication(), "apk:$packageName", !enabled)
    }

    fun installBuiltin(row: BuiltinPluginManager.Row) {
        executor.execute {
            val error = runCatching { builtin.install(row.id) }.exceptionOrNull()
            message.postValue(error?.let { "启用失败：${it.message}" } ?: "已启用 ${row.name}")
            builtinPlugins.postValue(builtin.rows())
            sources.postValue(MarketSources.all(ctx()))
            uiPlugins.postValue(uiPluginManager.installed())
            if (error == null) reloadUi.postValue(true)
        }
    }


    /** 从市场索引安装/更新界面插件（相对 URL 按索引地址解析）。 */
    fun installUiPluginFromUrl(entry: UiMarketPlugin, indexBase: String?) {
        executor.execute {
            val app = getApplication<Application>()
            val error = runCatching { installUiBlocking(entry, indexBase) }.exceptionOrNull()
            message.postValue(
                error?.let { app.getString(com.stupidtree.hitax.R.string.plugin_ui_install_failed, it.message ?: "") }
                    ?: app.getString(com.stupidtree.hitax.R.string.plugin_ui_installed, entry.name)
            )
            uiPlugins.postValue(uiPluginManager.installed())
        }
    }

    private fun installUiBlocking(
        entry: UiMarketPlugin,
        indexBase: String?,
        reloadOnMainTab: Boolean = true,
    ): InstalledUiPlugin {
        val app = getApplication<Application>()
        require(entry.sha256.matches(Regex("^[a-fA-F0-9]{64}$"))) {
            "索引缺少有效的 sha256，拒绝安装"
        }
        val minApi = entry.minHostApi ?: 1
        require(minApi <= HITA_API_VERSION) {
            "插件需要宿主 API $minApi，当前 ${HITA_API_VERSION}"
        }
        val file = java.io.File(app.cacheDir, "ui-${entry.id}.apk")
        PluginFetch.download(entry.url, indexBase, file)
        return try {
            val installed = uiPluginManager.installFile(file, entry.sha256)
            MarketSources.recordInstall(ctx(), installed.id, activeSource().url)
            if (reloadOnMainTab && hasMainTab(installed)) reloadUi.postValue(true)
            installed
        } finally {
            file.delete()
        }
    }


    /**
     * 应用整合包：按清单安装缺失插件；已安装的跳过。
     * 已停用的内置插件会重新启用；未受信任的 dex 插件不会静默安装。
     */
    fun applyPreset(preset: MarketPreset) {
        val previous = activePresetId.value
            ?.let { id -> presets.value?.firstOrNull { it.id == id } }
            ?.takeIf { it.id != preset.id }
        executor.execute {
            val minHost = preset.minHostApi ?: 1
            if (minHost > HITA_API_VERSION) {
                message.postValue("整合包需要宿主 API $minHost，当前 $HITA_API_VERSION")
                return@execute
            }
            val ui = availableUiPlugins.value.orEmpty().associateBy { it.id }
            val dex = items.value.orEmpty().associateBy { it.entry.id }
            val installedUi = uiPluginManager.installed().associateBy { it.id }
            val installedDex = installer.installed().associateBy { it.id }
            val builtins = builtin.rows().associateBy { it.id }
            var installed = 0
            var enabled = 0
            var skipped = 0
            var reloadNeeded = false
            val failures = mutableListOf<String>()

            for (ref in preset.refs()) {
                when (ref.normalizedKind()) {
                    "ui" -> {
                        val row = installedUi[ref.id]
                        if (row != null) {
                            if (!row.enabled && uiPluginManager.setEnabled(ref.id, true)) {
                                enabled++
                                if (hasMainTab(row)) reloadNeeded = true
                            } else {
                                skipped++
                            }
                            continue
                        }
                        val entry = ui[ref.id]
                        if (entry == null) {
                            if (ref.isRequired()) failures += "${ref.id}（索引缺失）"
                            continue
                        }
                        runCatching { installUiBlocking(entry, lastIndexBase, reloadOnMainTab = false) }
                            .onSuccess { if (hasMainTab(it)) reloadNeeded = true; installed++ }
                            .onFailure { failures += "${entry.name}：${it.message}" }
                    }
                    "dex" -> {
                        val row = installedDex[ref.id]
                        if (row != null) {
                            if (!installer.isEnabled(ref.id) && runCatching {
                                    installer.setEnabled(ref.id, true)
                                }.isSuccess
                            ) {
                                enabled++
                            } else {
                                skipped++
                            }
                            continue
                        }
                        val entry = dex[ref.id]?.entry
                        if (entry == null) {
                            if (ref.isRequired()) failures += "${ref.id}（索引缺失）"
                            continue
                        }
                        if (!trustStore.isVerified(entry.id)) {
                            failures += "${entry.name}（未受信任，需手动安装）"
                            continue
                        }
                        runCatching { installDexBlocking(entry) }
                            .onSuccess { installed++ }
                            .onFailure { failures += "${entry.name}：${it.message}" }
                    }
                    "builtin" -> {
                        val row = builtins[ref.id]
                        if (row == null) {
                            if (ref.isRequired()) failures += "${ref.id}（内置缺失）"
                            continue
                        }
                        if (row.installed) skipped++ else {
                            runCatching { builtin.setEnabled(ref.id, true) }
                                .onSuccess { reloadNeeded = true; installed++ }
                                .onFailure { failures += "${row.name}：${it.message}" }
                        }
                    }
                    else -> if (ref.isRequired()) failures += "${ref.id}（未知类型）"
                }
            }

            var disabledOld = 0
            if (failures.isEmpty() && previous != null) {
                val keep = preset.refs().map { it.normalizedKind() to it.id }.toSet()
                val installedUiNow = uiPluginManager.installed().associateBy { it.id }
                val builtinsNow = builtin.rows().associateBy { it.id }
                previous.refs()
                    .filter { (it.normalizedKind() to it.id) !in keep }
                    .forEach { ref ->
                        when (ref.normalizedKind()) {
                            "ui" -> {
                                val row = installedUiNow[ref.id] ?: return@forEach
                                if (row.enabled && uiPluginManager.setEnabled(ref.id, false)) {
                                    disabledOld++
                                    reloadNeeded = true
                                }
                            }
                            "builtin" -> {
                                val row = builtinsNow[ref.id] ?: return@forEach
                                if (row.installed && builtin.setEnabled(ref.id, false).isSuccess) {
                                    disabledOld++
                                    reloadNeeded = true
                                }
                            }
                        }
                    }
            }
            if (failures.isEmpty()) {
                PresetStateStore.setActive(ctx(), preset.id)
                activePresetId.postValue(preset.id)
            }

            val summary = buildString {
                append("整合包 ${preset.name}：安装 $installed")
                if (enabled > 0) append("，启用 $enabled")
                append("，跳过 $skipped")
                if (disabledOld > 0) append("，停用旧组合 $disabledOld")
                if (failures.isNotEmpty()) append("，失败 ${failures.size}")
            }
            message.postValue(if (failures.isEmpty()) summary else summary + "；" + failures.joinToString("；"))
            if (reloadNeeded) reloadUi.postValue(true)
            refresh()
        }
    }

    /** 一键更新：每个插件优先回到它安装时的索引源解析新版本。 */
    fun updateAll() {
        executor.execute {
            val app = getApplication<Application>()
            val active = MarketSources.active(app)
            val uiInstalled = uiPluginManager.installed()
            val dexInstalled = installer.installed()

            fun sourceOf(id: String): MarketSource {
                val recorded = MarketSources.installSource(app, id)
                return if (recorded.isNullOrBlank()) active else MarketSource("recorded", recorded)
            }

            val sources = (uiInstalled.map { it.id } + dexInstalled.map { it.id })
                .map { sourceOf(it) }
                .distinctBy { it.url }
            val indexes = sources.associate { source ->
                source.url to runCatching { readIndex(source) }.getOrNull()
            }

            var ok = 0
            var fail = 0
            val beforeNav = navIds()
            uiInstalled.forEach { installed ->
                val source = sourceOf(installed.id)
                val index = indexes[source.url] ?: return@forEach
                val entry = index.first.uiPlugins.firstOrNull { it.id == installed.id }
                    ?: return@forEach
                if (!PluginVersion.isNewer(entry.version, installed.version)) return@forEach
                runCatching {
                    val minApi = entry.minHostApi ?: 1
                    require(minApi <= HITA_API_VERSION) {
                        "插件需要宿主 API $minApi，当前 ${HITA_API_VERSION}"
                    }
                    val file = java.io.File(app.cacheDir, "ui-${entry.id}.apk")
                    PluginFetch.download(entry.url, source.url, file)
                    try {
                        uiPluginManager.installFile(file, entry.sha256)
                    } finally {
                        file.delete()
                    }
                }.onSuccess { ok++ }.onFailure { fail++ }
            }
            dexInstalled.forEach { installed ->
                val source = sourceOf(installed.id)
                val index = indexes[source.url] ?: return@forEach
                val entry = index.first.plugins.firstOrNull { it.id == installed.id }
                    ?: return@forEach
                if (!PluginVersion.isNewer(entry.version, installed.version)) return@forEach
                runCatching {
                    val minApi = entry.minHostApi ?: 1
                    require(minApi <= HITA_API_VERSION) {
                        "插件需要宿主 API $minApi，当前 ${HITA_API_VERSION}"
                    }
                    installer.install(entry, source.url)
                }.onSuccess { ok++ }.onFailure { fail++ }
            }
            uiPlugins.postValue(uiPluginManager.installed())
            val updatedMainTab = uiInstalled.any { installed ->
                hasMainTab(installed) &&
                    (indexes[sourceOf(installed.id).url]?.first?.uiPlugins
                        ?.firstOrNull { it.id == installed.id }
                        ?.let { PluginVersion.isNewer(it.version, installed.version) } == true)
            }
            if (updatedMainTab) reloadUi.postValue(true)
            message.postValue(
                when {
                    ok == 0 && fail == 0 -> "没有可更新的插件"
                    fail == 0 -> "已更新 $ok 个插件"
                    else -> "更新完成：成功 $ok，失败 $fail"
                }
            )
            refresh()
        }
    }

    fun uninstallUiPlugin(plugin: InstalledUiPlugin) {
        executor.execute {
            runCatching { uiPluginManager.uninstall(plugin.id) }
            uiPlugins.postValue(uiPluginManager.installed())
            if (hasMainTab(plugin)) reloadUi.postValue(true)
            message.postValue("已卸载 ${plugin.name}")
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

    /**
     * @return 索引内容、解析相对地址的基准、以及是否来自本地缓存。
     * 远端读取失败时回退到上次成功缓存，缓存也没有才抛错。
     */
    private fun readIndex(): Triple<MarketIndex, String?, Boolean> =
        readIndex(MarketSources.active(ctx()))

    private fun readIndex(source: MarketSource): Triple<MarketIndex, String?, Boolean> {
        val ctx = getApplication<Application>()
        if (source.url.isBlank()) {
            val file = File(ctx.filesDir, "market/index.json")
            return Triple(
                gson.fromJson(file.readText(), MarketIndex::class.java),
                file.parentFile.absolutePath,
                false,
            )
        }
        val cache = cacheDirFor(source.url)
        return runCatching {
            val text = fetchIndex(source.url)
            IndexCache.save(cache, source.url, text)
            Triple(
                gson.fromJson(text, MarketIndex::class.java),
                source.url,
                false,
            )
        }.getOrElse { error ->
            val cached = IndexCache.load(cache) ?: throw error
            Triple(
                gson.fromJson(cached.raw, MarketIndex::class.java),
                cached.base,
                true,
            )
        }
    }

    private fun fetchIndex(url: String): String {
        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
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
