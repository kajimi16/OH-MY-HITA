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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
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

    data class SwitchProgress(val percent: Int, val message: String)

    data class PresetSwitchTarget(val presetId: String, val shell: String?)

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

    /** 界面插件 prepare 阶段的 staging 根目录；cleanup 时整体删除。 */
    private val uiStagingRoot = File(application.cacheDir, "ui-staging")

    /** 运行时界面插件（Compose 容器，独立 APK）。 */
    val uiPlugins = MutableLiveData<List<InstalledUiPlugin>>(emptyList())

    /** 索引里可安装 / 可更新的界面插件。 */
    val availableUiPlugins = MutableLiveData<List<UiMarketPlugin>>(emptyList())

    /** 索引里的整合包（可选字段）。 */
    val presets = MutableLiveData<List<MarketPreset>>(emptyList())

    /** 当前生效的整合包 id；切换成功后更新。 */
    val activePresetId = MutableLiveData<String?>(PresetStateStore.activeId(application))

    /** 0–100 的切换进度；null 表示没有正在进行的切换。 */
    val applyProgress = MutableLiveData<SwitchProgress?>(null)

    /** 一次性成功事件：市场据此启动目标 UI 并关闭自己。 */
    val presetSwitch = MutableLiveData<PresetSwitchTarget?>(null)

    fun consumePresetSwitch() {
        presetSwitch.value = null
    }

    /** 索引是否已完成一次读取（成功或失败），用于区分“还在加载”和“没有预设”。 */
    val presetsLoaded = MutableLiveData(false)

    /** 可选插件源（默认源 + 自定义源）。 */
    val sources = MutableLiveData<List<MarketSource>>(emptyList())

    /** 其它来源（非当前源）里可更新的插件数量。 */
    val pendingUpdates = MutableLiveData(0)
    val pluginStats = MutableLiveData<Map<String, MarketStat>>(emptyMap())
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
                    backfillLayoutIfNeeded(index.presets.orEmpty())
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

    /**
     * 存量补写：老版本宿主、或索引尚未发布 tabs 时装过整合包的用户，
     * 本地只有 preset_id、没有 tabs_json。索引补上 tabs 后在这里补写一次，
     * 避免必须手动重新应用整合包才能看到新布局。
     */
    fun refreshPluginStats(ids: List<String>) {
        if (ids.isEmpty()) return
        executor.execute {
            val url = URL(
                com.stupidtree.hitax.BuildConfig.USAGE_BASE_URL.trimEnd('/') + "/api/market/stats?ids=" +
                    URLEncoder.encode(ids.take(50).joinToString(","), "UTF-8")
            )
            runCatching {
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8_000
                conn.readTimeout = 10_000
                conn.setRequestProperty("Accept", "application/json")
                try {
                    if (conn.responseCode !in 200..299) return@runCatching
                    val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    val stats = root.optJSONObject("stats") ?: return@runCatching
                    val out = LinkedHashMap<String, MarketStat>()
                    stats.keys().forEach { id ->
                        val obj = stats.optJSONObject(id) ?: return@forEach
                        out[id] = MarketStat(
                            downloads = obj.optLong("downloads", 0),
                            activeUsers = obj.optLong("activeUsers", 0),
                        )
                    }
                    pluginStats.postValue(out)
                } finally {
                    conn.disconnect()
                }
            }
        }
    }

    private fun backfillLayoutIfNeeded(presets: List<MarketPreset>) {
        val activeId = PresetStateStore.activeId(ctx()) ?: return
        val tabs = PresetLayoutSync.tabsToBackfill(activeId, PresetLayoutStore.tabs(ctx()), presets) ?: return
        PresetLayoutStore.save(ctx(), activeId, tabs)
        reloadUi.postValue(true)
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
            CompositionStore.clearPresetDisabled(
                ctx(),
                CompositionTarget(CompositionKind.BUILTIN, row.id),
            )
            val error = runCatching { builtin.setEnabled(row.id, enabled) }.exceptionOrNull()
            message.postValue(error?.let { "操作失败：${it.message}" } ?: null)
            builtinPlugins.postValue(builtin.rows())
            if (error == null) reloadUi.postValue(true)
        }
    }

    fun setUiPluginEnabled(plugin: InstalledUiPlugin, enabled: Boolean) {
        executor.execute {
            CompositionStore.clearPresetDisabled(
                ctx(),
                CompositionTarget(CompositionKind.UI, plugin.id),
            )
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
            CompositionStore.clearPresetDisabled(
                ctx(),
                CompositionTarget(CompositionKind.DEX, entry.id),
            )
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

    /**
     * 准备阶段：下载界面插件 APK 到 staging 并校验 sha256；不触碰已安装版本。
     */
    private fun prepareUi(entry: UiMarketPlugin, indexBase: String?): File {
        require(entry.sha256.matches(Regex("^[a-fA-F0-9]{64}$"))) {
            "索引缺少有效的 sha256，拒绝安装"
        }
        val minApi = entry.minHostApi ?: 1
        require(minApi <= HITA_API_VERSION) {
            "插件需要宿主 API $minApi，当前 ${HITA_API_VERSION}"
        }
        val dir = File(uiStagingRoot, entry.id)
        dir.deleteRecursively()
        dir.mkdirs()
        val file = File(dir, "plugin.apk")
        try {
            PluginFetch.download(entry.url, indexBase, file)
            val actual = uiPluginManager.sha256Of(file)
            require(actual.equals(entry.sha256, ignoreCase = true)) {
                "sha256 校验失败：期望 ${entry.sha256}，实际 $actual"
            }
            return file
        } catch (t: Throwable) {
            dir.deleteRecursively()
            throw t
        }
    }

    /** 把已准备好的文件装进 ui_plugins 并记录来源；不访问网络。 */
    private fun attachUi(
        entry: UiMarketPlugin,
        file: File,
        reloadOnMainTab: Boolean = true,
        sourceUrl: String = activeSource().url,
    ): InstalledUiPlugin {
        val installed = uiPluginManager.installFile(file, entry.sha256)
        MarketSources.recordInstall(ctx(), installed.id, sourceUrl)
        if (reloadOnMainTab && hasMainTab(installed)) reloadUi.postValue(true)
        return installed
    }

    /** 兼容包装：prepare + attach，并清理 staging。 */
    private fun installUiBlocking(
        entry: UiMarketPlugin,
        indexBase: String?,
        reloadOnMainTab: Boolean = true,
    ): InstalledUiPlugin {
        val file = prepareUi(entry, indexBase)
        return try {
            attachUi(entry, file, reloadOnMainTab)
        } finally {
            file.parentFile?.deleteRecursively()
        }
    }


    /**
     * 应用整合包：按清单安装缺失插件；已安装的跳过。
     * 已停用的内置插件会重新启用；未受信任的 dex 插件不会静默安装。
     */
    fun applyPreset(preset: MarketPreset) {
        executor.execute {
            fun fail(text: String) {
                applyProgress.postValue(null)
                message.postValue(text)
            }
            val switchStartedAt = android.os.SystemClock.elapsedRealtime()
            applyProgress.postValue(SwitchProgress(5, "正在准备切换…"))
            val minHost = preset.minHostApi ?: 1
            if (minHost > HITA_API_VERSION) {
                fail("整合包需要宿主 API $minHost，当前 $HITA_API_VERSION")
                return@execute
            }
            if (preset.refs().isEmpty()) {
                fail("整合包没有插件清单，已取消")
                return@execute
            }
            val invalidKinds = preset.refs()
                .map { it.normalizedKind() }
                .filterNot { it in setOf("ui", "dex", "builtin") }
                .distinct()
            if (invalidKinds.isNotEmpty()) {
                fail("整合包含未知插件类型：${invalidKinds.joinToString()}")
                return@execute
            }
            applyProgress.postValue(SwitchProgress(15, "正在计算组合差异…"))
            // 在单线程执行器内读取，连续点击时第二次能看到第一次刚写入的 active preset。
            val activeId = PresetStateStore.activeId(ctx())
            val previous = previousPresetFor(
                activeId = activeId,
                nextId = preset.id,
                live = activeId?.let { id -> presets.value?.firstOrNull { it.id == id } },
                saved = CompositionStore.savedPreset(ctx()),
            )
            val ui = availableUiPlugins.value.orEmpty().associateBy { it.id }
            val dex = items.value.orEmpty().associateBy { it.entry.id }
            val installedUi = uiPluginManager.installed().associateBy { it.id }
            val installedDex = installer.installed().associateBy { it.id }
            val builtins = builtin.rows().associateBy { it.id }
            val userDisabled = mapOf(
                CompositionKind.UI to CompositionStore.effectiveUserDisabled(
                    ctx(),
                    CompositionKind.UI,
                    installedUi.values.filterNot { it.enabled }.map { it.id }.toSet(),
                ),
                CompositionKind.DEX to CompositionStore.effectiveUserDisabled(
                    ctx(),
                    CompositionKind.DEX,
                    installedDex.keys.filterNot { installer.isEnabled(it) }.toSet(),
                ),
                CompositionKind.BUILTIN to CompositionStore.effectiveUserDisabled(
                    ctx(),
                    CompositionKind.BUILTIN,
                    builtins.keys.filter { PluginPrefs.isDisabled(ctx(), it) }.toSet(),
                ),
            )
            val plan = CompositionResolver.resolve(
                baseIds = builtins.keys,
                next = preset,
                previous = previous,
                available = mapOf(
                    CompositionKind.UI to ui.keys,
                    CompositionKind.DEX to dex.keys,
                    CompositionKind.BUILTIN to builtins.keys,
                ),
                installed = mapOf(
                    CompositionKind.UI to installedUi.keys,
                    CompositionKind.DEX to installedDex.keys,
                    CompositionKind.BUILTIN to builtins.values
                        .filter { it.installed }
                        .map { it.id }
                        .toSet(),
                ),
                userDisabled = userDisabled,
                killSwitches = CompositionStore.killSwitches(ctx()),
            )
            val requiredMissing = plan.blockingRequiredMissing()
            if (requiredMissing.isNotEmpty()) {
                fail(
                    "整合包缺少必选插件：" + requiredMissing.joinToString {
                        "${it.id}（${it.kind.name.lowercase()}）"
                    },
                )
                return@execute
            }
            val requiredDisabled = plan.blockingRequiredDisabled()
            if (requiredDisabled.isNotEmpty()) {
                fail(
                    "整合包必选插件被停用：" + requiredDisabled.joinToString {
                        "${it.id}（${it.source.name.lowercase()}）"
                    },
                )
                return@execute
            }
            val state = CompositionState(
                installed = mapOf(
                    CompositionKind.UI to installedUi.keys,
                    CompositionKind.DEX to installedDex.keys,
                    CompositionKind.BUILTIN to builtins.values
                        .filter { it.installed }
                        .map { it.id }
                        .toSet(),
                ),
                enabled = mapOf(
                    CompositionKind.UI to installedUi.values
                        .filter { it.enabled }
                        .map { it.id }
                        .toSet(),
                    CompositionKind.DEX to installedDex.keys
                        .filter { installer.isEnabled(it) }
                        .toSet(),
                    CompositionKind.BUILTIN to builtins.values
                        .filter { it.installed }
                        .map { it.id }
                        .toSet(),
                ),
            )
            CompositionStore.record(ctx(), plan, state)
            applyProgress.postValue(SwitchProgress(35, "正在安装/启用目标插件…"))

            // 准备阶段只下载/校验/落 staging；任何必选失败都在 reconcile 之前中止。
            val artifacts = mutableMapOf<CompositionTarget, PreparedArtifact>()
            val materializer = object : CompositionMaterializer {
                override fun prepare(
                    kind: CompositionKind,
                    id: String,
                ): Result<PreparedArtifact> = runCatching {
                    when (kind) {
                        CompositionKind.UI -> {
                            val entry = ui[id] ?: error("索引缺失")
                            val file = prepareUi(entry, lastIndexBase)
                            PreparedArtifact.Ui(entry, file, entry.sha256)
                        }
                        CompositionKind.DEX -> {
                            val entry = dex[id]?.entry ?: error("索引缺失")
                            if (!trustStore.isVerified(entry.id)) {
                                error("未受信任，需手动安装")
                            }
                            installer.prepare(entry, lastIndexBase)
                        }
                        CompositionKind.BUILTIN -> error("内置插件不需要 artifact")
                    }
                }.onSuccess { artifacts[CompositionTarget(kind, id)] = it }

                override fun cleanup() {
                    runCatching { installer.cleanupStaging() }
                    uiStagingRoot.deleteRecursively()
                    artifacts.clear()
                }
            }

            val compositionExecutor = object : CompositionExecutor {
                override fun install(kind: CompositionKind, id: String): Result<Boolean> {
                    return when (kind) {
                        CompositionKind.UI -> {
                            val artifact = artifacts[CompositionTarget(kind, id)]
                                as? PreparedArtifact.Ui
                                ?: return Result.failure(IllegalStateException("界面插件未准备"))
                            runCatching {
                                attachUi(artifact.entry, artifact.file, reloadOnMainTab = false)
                            }.map { hasMainTab(it) }
                        }
                        CompositionKind.DEX -> {
                            val artifact = artifacts[CompositionTarget(kind, id)]
                                as? PreparedArtifact.Dex
                                ?: return Result.failure(IllegalStateException("插件未准备"))
                            val before = navIds()
                            runCatching { installer.attach(artifact); navIds() != before }
                        }
                        CompositionKind.BUILTIN -> runCatching {
                            builtin.setEnabled(id, true).getOrThrow()
                            true
                        }
                    }
                }

                override fun enable(kind: CompositionKind, id: String): Result<Boolean> = when (kind) {
                    CompositionKind.UI -> {
                        val row = installedUi[id]
                            ?: uiPluginManager.installed().firstOrNull { it.id == id }
                        if (row != null && uiPluginManager.setEnabled(id, true)) {
                            Result.success(hasMainTab(row))
                        } else {
                            Result.failure(IllegalStateException("启用失败"))
                        }
                    }
                    CompositionKind.DEX -> {
                        val before = navIds()
                        runCatching {
                            installer.setEnabled(id, true)
                            navIds() != before
                        }
                    }
                    CompositionKind.BUILTIN -> builtin.setEnabled(id, true).map { true }
                }

                override fun restore(
                    kind: CompositionKind,
                    id: String,
                    installed: Boolean,
                    enabled: Boolean,
                ): Result<Boolean> = when (kind) {
                    CompositionKind.UI -> runCatching {
                        if (!installed) {
                            uiPluginManager.uninstall(id)
                            true
                        } else {
                            uiPluginManager.setEnabled(id, enabled)
                        }
                    }
                    CompositionKind.DEX -> runCatching {
                        if (!installed) {
                            installer.uninstall(id)
                            true
                        } else if (installer.isEnabled(id) == enabled) {
                            false
                        } else {
                            installer.setEnabled(id, enabled)
                            true
                        }
                    }
                    CompositionKind.BUILTIN -> runCatching {
                        if (!installed) {
                            builtin.uninstall(id, purge = false).getOrThrow()
                            true
                        } else {
                            builtin.setEnabled(id, enabled).getOrThrow()
                            true
                        }
                    }
                }

                override fun disable(kind: CompositionKind, id: String): Result<Boolean> = when (kind) {
                    CompositionKind.UI ->
                        if (uiPluginManager.setEnabled(id, false)) {
                            Result.success(true)
                        } else {
                            Result.failure(IllegalStateException("停用失败"))
                        }
                    CompositionKind.DEX -> {
                        val before = navIds()
                        runCatching {
                            installer.setEnabled(id, false)
                            navIds() != before
                        }
                    }
                    CompositionKind.BUILTIN -> builtin.setEnabled(id, false).map { true }
                }
            }
            applyProgress.postValue(SwitchProgress(55, "正在停用旧组合并启用新组合…"))
            val report = CompositionReconciler.executePrepared(
                plan = plan.forPresetApplication(),
                state = state,
                materializer = materializer,
                executor = compositionExecutor,
            )
            CompositionStore.recordResult(ctx(), plan, state, report)
            applyProgress.postValue(SwitchProgress(85, "正在保存组合状态…"))
            val installed = report.installed
            val enabled = report.enabled
            val skipped = report.skipped
            val disabledOld = report.disabled
            val targetShell = PresetStateStore.shellForPreset(preset.id, preset.shell)
            val reloadNeeded = report.reloadNeeded || previous?.shell != targetShell
            val failures = report.failures
            val optionalFailures = report.optionalFailures
            if (report.canActivate) {
                // active preset / shell / tabs 必须一次提交；否则冷启动可能读到半状态。
                val committed = PresetStateStore.saveUi(
                    ctx(),
                    preset.id,
                    targetShell,
                    preset.tabs,
                )
                if (!committed) {
                    fail("切换失败：无法保存整合包状态，已保留旧组合")
                    refresh()
                    return@execute
                }
                CompositionStore.savePreset(ctx(), preset)
                activePresetId.postValue(preset.id)
                // 切换通常很快；至少让用户看到进度窗口，避免一闪而过。
                val remain = 650 - (android.os.SystemClock.elapsedRealtime() - switchStartedAt)
                if (remain > 0) Thread.sleep(remain)
                applyProgress.postValue(SwitchProgress(100, "正在切换界面…"))
                // HITA-Android 的 shell 是宿主映射出来的，不要只按 preset.shell 判断。
                presetSwitch.postValue(
                    PresetSwitchTarget(preset.id, PresetStateStore.activeShell(ctx())),
                )
            } else {
                applyProgress.postValue(null)
            }

            val summary = buildString {
                append("整合包 ${preset.name}：安装 $installed")
                if (enabled > 0) append("，启用 $enabled")
                append("，跳过 $skipped")
                if (disabledOld > 0) append("，停用旧组合 $disabledOld")
                if (report.rolledBack > 0) append("，回滚 ${report.rolledBack}")
                val allFailures = failures + optionalFailures + report.rollbackFailures
                if (allFailures.isNotEmpty()) append("，失败 ${allFailures.size}")
            }
            val allFailures = report.allFailures
            message.postValue(if (allFailures.isEmpty()) summary else summary + "；" + allFailures.joinToString("；"))
            if (reloadNeeded && !report.canActivate) reloadUi.postValue(true)
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
                    val file = prepareUi(entry, source.url)
                    try {
                        attachUi(entry, file, reloadOnMainTab = false, sourceUrl = source.url)
                    } finally {
                        file.parentFile?.deleteRecursively()
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
