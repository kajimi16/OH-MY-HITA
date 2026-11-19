package com.stupidtree.hitax

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import android.util.Log
import androidx.annotation.WorkerThread
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.stupidtree.stupiduser.data.repository.LocalUserRepository
import com.stupidtree.sync.StupidSync
import com.stupidtree.hita.api.EasService
import com.stupidtree.hita.api.HostUiService
import com.stupidtree.hita.api.ThemeController
import com.stupidtree.hita.kernel.PluginHost
import com.stupidtree.hitax.market.CompositionExecutor
import com.stupidtree.hitax.market.CompositionKind
import com.stupidtree.hitax.market.CompositionState
import com.stupidtree.hitax.market.CompositionStore
import com.stupidtree.hitax.market.IndexCache
import com.stupidtree.hitax.market.MarketIndex
import com.stupidtree.hitax.market.MarketSources
import com.stupidtree.hitax.market.PluginInstaller
import com.stupidtree.hitax.market.StartupOutcome
import com.stupidtree.hitax.market.diagnosticLines
import com.stupidtree.hitax.market.executeStartup
import com.stupidtree.hitax.market.planStartup
import com.stupidtree.hitax.plugin.BuiltinPluginManager
import com.stupidtree.hitax.plugin.PluginCatalog
import com.stupidtree.hitax.plugin.PluginPrefs
import com.stupidtree.hitax.uiplugin.UiPluginManager
import com.stupidtree.hitax.data.work.CourseReminderScheduler
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class HApplication : Application() {

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    /** 微内核。插件只通过它注册服务与导航项。 */
    lateinit var pluginHost: PluginHost
        private set

    override fun onCreate() {
        super.onCreate()
        // PDFBox assets must be registered before any attachment parser runs.
        runCatching { com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(this) }
        // 上次卸载时请求清除的数据，必须在任何插件装配、界面创建之前处理掉
        com.stupidtree.hitax.plugin.PluginPurge.runPending(this)
        // 万物皆插件：先装配内核，再初始化内核自身依赖的业务。
        pluginHost = PluginHost(
            pluginsDir = File(filesDir, "plugins"),
            logger = { message, error ->
                if (error == null) Log.i(TAG, message) else Log.e(TAG, message, error)
            },
        )
        // 宿主内建能力：插件只能通过契约使用，不能直接碰 Android 主题 API。
        pluginHost.provideGlobal(ThemeController.KEY, object : ThemeController {
            override fun setNightMode(night: Boolean) {
                // AppCompatDelegate 必须在主线程调用；插件安装发生在后台线程。
                mainHandler.post {
                    // 应用有自己的主题模式（theme 偏好），必须先改它，否则
                    // BaseActivity.onCreate 会按旧模式再设一遍而覆盖掉这里的选择。
                    com.stupidtree.style.ThemeTools.setThemeMode(
                        this@HApplication,
                        if (night) com.stupidtree.style.ThemeTools.MODE.DARK
                        else com.stupidtree.style.ThemeTools.MODE.LIGHT
                    )
                    AppCompatDelegate.setDefaultNightMode(
                        if (night) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                    )
                }
            }

            override fun setColorPreset(preset: String): Boolean =
                com.stupidtree.style.ThemeTools.setColorPreset(this@HApplication, preset)

            override fun colorPreset(): String =
                com.stupidtree.style.ThemeTools.getColorPreset(this@HApplication)

            override fun setThemeStyle(style: String): Boolean {
                val mapped = when (style) {
                    "apple_glass" -> com.stupidtree.style.ThemeTools.STYLE.APPLE_GLASS
                    "cyber" -> com.stupidtree.style.ThemeTools.STYLE.CYBER
                    "sora_cloud" -> com.stupidtree.style.ThemeTools.STYLE.SORA_CLOUD
                    "p5" -> com.stupidtree.style.ThemeTools.STYLE.P5
                    "deep_space" -> com.stupidtree.style.ThemeTools.STYLE.DEEP_SPACE
                    "sumi" -> com.stupidtree.style.ThemeTools.STYLE.SUMI
                    "classic" -> com.stupidtree.style.ThemeTools.STYLE.CLASSIC
                    else -> return false
                }
                return com.stupidtree.style.ThemeTools.setThemeStyle(this@HApplication, mapped)
            }

            override fun themeStyle(): String =
                when (com.stupidtree.style.ThemeTools.getThemeStyle(this@HApplication)) {
                    com.stupidtree.style.ThemeTools.STYLE.APPLE_GLASS -> "apple_glass"
                    com.stupidtree.style.ThemeTools.STYLE.CYBER -> "cyber"
                    com.stupidtree.style.ThemeTools.STYLE.SORA_CLOUD -> "sora_cloud"
                    com.stupidtree.style.ThemeTools.STYLE.P5 -> "p5"
                    com.stupidtree.style.ThemeTools.STYLE.DEEP_SPACE -> "deep_space"
                    com.stupidtree.style.ThemeTools.STYLE.SUMI -> "sumi"
                    else -> "classic"
                }

            override fun setFontScale(scale: Float): Boolean =
                com.stupidtree.style.ThemeTools.setFontScale(this@HApplication, scale)

            override fun fontScale(): Float =
                com.stupidtree.style.ThemeTools.getFontScale(this@HApplication)

            override fun tokens(): com.stupidtree.hita.api.ThemeTokens =
                when (com.stupidtree.style.ThemeTools.getThemeStyle(this@HApplication)) {
                    com.stupidtree.style.ThemeTools.STYLE.APPLE_GLASS ->
                        com.stupidtree.hita.api.ThemeTokens(24f, 20f, 0.72f)
                    com.stupidtree.style.ThemeTools.STYLE.CYBER ->
                        com.stupidtree.hita.api.ThemeTokens(8f, 12f, 0.85f)
                    com.stupidtree.style.ThemeTools.STYLE.SORA_CLOUD ->
                        com.stupidtree.hita.api.ThemeTokens(20f, 16f, 1f)
                    com.stupidtree.style.ThemeTools.STYLE.P5 ->
                        com.stupidtree.hita.api.ThemeTokens(4f, 8f, 1f)
                    com.stupidtree.style.ThemeTools.STYLE.DEEP_SPACE ->
                        com.stupidtree.hita.api.ThemeTokens(18f, 16f, 0.9f)
                    com.stupidtree.style.ThemeTools.STYLE.SUMI ->
                        com.stupidtree.hita.api.ThemeTokens(10f, 14f, 1f)
                    else -> com.stupidtree.hita.api.ThemeTokens(16f, 16f, 1f)
                }
        })
        // 宿主内建提醒能力：插件不能自己声明 receiver / 前台服务，统一交给宿主调度。
        pluginHost.provideGlobal(
            com.stupidtree.hita.api.ReminderService.KEY,
            com.stupidtree.hitax.reminder.ReminderServiceImpl(this),
        )
        pluginHost.provideGlobal(
            com.stupidtree.hita.api.LiveCourseService.KEY,
            com.stupidtree.hitax.livecourse.LiveCourseServiceImpl(this),
        )
        // 宿主后台任务：WorkManager 负责持久化，插件只实现 BackgroundTask。
        pluginHost.provideGlobal(
            com.stupidtree.hita.api.BackgroundTaskService.KEY,
            com.stupidtree.hitax.background.BackgroundTaskServiceImpl(this),
        )
        // 宿主内建跳转能力：迁移期插件请求宿主打开课表详情 / 教务导入
        pluginHost.provideGlobal(HostUiService.KEY, object : HostUiService {
            override fun openTimetableDetail(context: android.content.Context, timetableId: String) {
                com.stupidtree.hitax.utils.ActivityUtils.startTimetableDetailActivity(context, timetableId)
            }

            override fun openImportTimetable(context: android.content.Context) {
                (context as? androidx.fragment.app.FragmentActivity)?.let { activity ->
                    pluginHost.service(EasService.KEY)?.openImportTimetable(activity)
                }
            }

            override fun openCourseResourceSearch(context: android.content.Context, keyword: String?, submit: Boolean) {
                pluginHost.service(com.stupidtree.hita.api.ResourceUiService.KEY)
                    ?.openSearch(context, keyword, submit)
            }

            override fun openTeacherHomepage(context: android.content.Context, teacherName: String) {
                pluginHost.service(com.stupidtree.hita.api.ResourceUiService.KEY)
                    ?.openTeacherHomepage(context, teacherName)
            }

            override fun openAddEvent(
                activity: android.app.Activity,
                timetable: com.stupidtree.hitax.data.model.timetable.Timetable?,
                dow: Int,
                week: Int,
                period: com.stupidtree.hitax.data.model.timetable.TimePeriodInDay?,
                subject: com.stupidtree.hitax.data.model.timetable.TermSubject?,
            ) {
                pluginHost.service(com.stupidtree.hita.api.TimetableUiService.KEY)
                    ?.openAddEvent(activity, timetable, dow, week, period, subject)
            }

            override fun openCourseReadme(
                context: android.content.Context,
                lifecycleOwner: androidx.lifecycle.LifecycleOwner,
                courseCode: String?,
                courseName: String?,
            ) {
                pluginHost.service(com.stupidtree.hita.api.ResourceUiService.KEY)
                    ?.openReadme(context, lifecycleOwner, courseCode, courseName)
            }

            override fun openCourseContribution(
                context: android.content.Context,
                repoName: String,
                courseName: String,
                courseCode: String,
                repoType: String,
            ) {
                pluginHost.service(com.stupidtree.hita.api.ResourceUiService.KEY)
                    ?.openContribution(context, repoName, courseName, courseCode, repoType)
            }

            override fun searchTeacherNames(query: String): androidx.lifecycle.LiveData<List<String>> {
                return pluginHost.service(com.stupidtree.hita.api.ResourceUiService.KEY)
                    ?.searchTeacherNames(query)
                    ?: androidx.lifecycle.MutableLiveData(emptyList())
            }

            override fun notifyWidgets() {
                pluginHost.service(com.stupidtree.hita.api.TimetableUiService.KEY)
                    ?.refreshWidgets(this@HApplication)
            }

            override fun startAutoReimport(callback: (Boolean) -> Unit) {
                pluginHost.service(EasService.KEY)?.startAutoReimport(callback) ?: callback(false)
            }

            override fun openUserProfile(context: android.content.Context, userId: String?) {
                pluginHost.service(com.stupidtree.hita.api.UserUiService.KEY)
                    ?.openProfile(context, userId)
            }

            override fun showEventInfo(
                context: android.content.Context,
                events: List<com.stupidtree.hitax.data.model.timetable.EventItem>,
            ) {
                (context as? androidx.fragment.app.FragmentActivity)?.let { act ->
                    pluginHost.service(com.stupidtree.hita.api.TimetableUiService.KEY)
                        ?.showEventInfo(act, events)
                }
            }
        })
        // 运行时界面插件入口：构建期插件打开功能前先问它，返回 false 再回退内置实现
        pluginHost.provideGlobal(com.stupidtree.hita.api.RuntimeUiService.KEY,
            object : com.stupidtree.hita.api.RuntimeUiService {
                override fun isAvailable(capability: String): Boolean =
                    com.stupidtree.hitax.uiplugin.UiPluginManager(this@HApplication)
                        .resolveForCapability(capability) != null

                override fun open(
                    context: android.content.Context,
                    capability: String,
                    arguments: android.os.Bundle,
                ): Boolean {
                    val record = com.stupidtree.hitax.uiplugin.UiPluginManager(this@HApplication)
                        .resolveForCapability(capability) ?: return false
                    val intent = android.content.Intent(
                        context,
                        com.stupidtree.hitax.uiplugin.PluginUiActivity::class.java,
                    )
                        .putExtra(
                            com.stupidtree.hitax.uiplugin.PluginUiActivity.EXTRA_PLUGIN_ID,
                            record.id,
                        )
                        .putExtra(
                            com.stupidtree.hitax.uiplugin.PluginUiActivity.EXTRA_ARGUMENTS,
                            arguments,
                        )
                    if (context !is android.app.Activity) {
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return true
                }
            })
        pluginHost.installAllWithDescriptors(PluginCatalog.installations(this))
        // 组件开关状态由系统持久化，可能与本地禁用标记不一致（如清除应用数据）；
        // 装配完成后按标记收敛一次，避免“插件已装配但组件仍禁用”。
        val builtinSafety = com.stupidtree.hitax.market.CompositionStore.killSwitches(this)
        com.stupidtree.hitax.plugin.PluginCatalog.entries(this).forEach { entry ->
            val enabled = !com.stupidtree.hitax.plugin.PluginPrefs.isDisabled(this, entry.id) &&
                com.stupidtree.hitax.market.CompositionTarget(
                    com.stupidtree.hitax.market.CompositionKind.BUILTIN,
                    entry.id,
                ) !in builtinSafety
            com.stupidtree.hitax.plugin.PluginComponents.setEnabled(this, entry.id, enabled)
        }
        // 恢复上次安装的运行时插件（T1）；单个失败不影响其它插件。
        com.stupidtree.hitax.market.PluginInstaller(this, pluginHost).restore()

        // A2：必须在内置装配与运行时插件恢复之后重放，否则 host.isInstalled() 为空，
        // 内置插件的 enable/disable 会退化成 no-op。启动期只对账，不下载。
        runCatching { replayActivePreset() }
            .onFailure { Log.e(TAG, "startup composition replay failed", it) }

        // 宿主不再认识任何具体数据表：只按 key 查 SyncCodec 并转发。
        StupidSync.init(this, object : StupidSync.PushDelegate {

            @WorkerThread
            override fun getDataForIds(key: String, ids: List<String>): List<JsonObject> =
                pluginHost.sync.get(key)?.export(ids).orEmpty()

            @WorkerThread
            override fun saveData(key: String, ids: List<String>, data: List<String>) {
                val expectedIds = ids.toList()
                val expectedData = data.toList()
                val original = com.stupidtree.hita.api.SyncRecord(key, ids.toList(), data.toList())
                val hooked = runCatching {
                    pluginHost.runHook(com.stupidtree.hita.api.HookKeys.SYNC_PULL_RECORD, original)
                }.getOrNull() ?: original
                // hook 只允许改写 data；重定向 key/ids 会破坏信任边界。
                val record = com.stupidtree.hitax.data.SyncHookPolicy.accept(
                    original,
                    expectedIds,
                    expectedData,
                    hooked,
                )
                pluginHost.sync.get(record.key)?.save(record.ids, record.data)
            }

            @WorkerThread
            override fun deleteData(key: String, ids: List<String>) {
                pluginHost.sync.get(key)?.delete(ids)
            }

            @WorkerThread
            override fun clearData(key: String) {
                pluginHost.sync.get(key)?.clear()
            }
        })
        StupidSync.setUID(LocalUserRepository.getInstance(this).getLoggedInUser().id)
        handleSSLHandshake()
        
        // 初始化课程提醒（根据用户设置自动调度或取消）
        CourseReminderScheduler.autoSchedule(this)
    }

    /**
     * 启动期按 active preset 重放一次组合。
     *
     * 不动网络、不装 DEX/UI：只对已安装/可装配的条目做 enable/disable。缓存索引缺失、
     * 必选条目缺失或被 USER/SAFETY 停用时，保持本次启动已有的组合并写诊断。
     */
    private fun replayActivePreset() {
        val saved = CompositionStore.savedPreset(this)
        if (saved == null) {
            CompositionStore.saveStartupOutcome(
                this,
                StartupOutcome(
                    attempted = false,
                    note = "没有已应用的整合包",
                    at = System.currentTimeMillis(),
                ),
            )
            return
        }
        val cached = IndexCache.loadStartup(filesDir, MarketSources.active(this).url)
        val index = cached?.let {
            runCatching { Gson().fromJson(it.raw, MarketIndex::class.java) }.getOrNull()
        }
        val uiManager = UiPluginManager(this)
        val installer = PluginInstaller(this, pluginHost)
        val builtin = BuiltinPluginManager(this)
        val uiInstalled = uiManager.installed()
        val dexInstalled = installer.installed()
        val builtinRows = builtin.rows()
        val builtinIds = builtinRows.map { it.id }.toSet()
        val available = mapOf(
            CompositionKind.UI to index?.uiPlugins.orEmpty().map { it.id }.toSet(),
            CompositionKind.DEX to index?.plugins.orEmpty().map { it.id }.toSet(),
            CompositionKind.BUILTIN to builtinIds,
        )
        val installed = mapOf(
            CompositionKind.UI to uiInstalled.map { it.id }.toSet(),
            CompositionKind.DEX to dexInstalled.map { it.id }.toSet(),
            // 内置插件代码随 APK 常驻；这里表示“可装配”，是否生效看 enabled。
            CompositionKind.BUILTIN to builtinIds,
        )
        val enabled = mapOf(
            CompositionKind.UI to uiInstalled.filter { it.enabled }.map { it.id }.toSet(),
            CompositionKind.DEX to dexInstalled
                .filter { installer.isEnabled(it.id) }
                .map { it.id }
                .toSet(),
            CompositionKind.BUILTIN to builtinRows
                .filter { it.installed }
                .map { it.id }
                .toSet(),
        )
        val userDisabled = mapOf(
            CompositionKind.UI to CompositionStore.effectiveUserDisabled(
                this,
                CompositionKind.UI,
                uiInstalled.filterNot { it.enabled }.map { it.id }.toSet(),
            ),
            CompositionKind.DEX to CompositionStore.effectiveUserDisabled(
                this,
                CompositionKind.DEX,
                dexInstalled.filterNot { installer.isEnabled(it.id) }.map { it.id }.toSet(),
            ),
            CompositionKind.BUILTIN to CompositionStore.effectiveUserDisabled(
                this,
                CompositionKind.BUILTIN,
                PluginPrefs.disabled(this),
            ),
        )
        val live = index?.presets?.firstOrNull { it.id == saved.id }
        val decision = planStartup(
            savedPreset = saved,
            livePreset = live,
            available = available,
            installed = installed,
            enabled = enabled,
            userDisabled = userDisabled,
            killSwitches = CompositionStore.killSwitches(this),
            indexAvailable = cached != null,
            appliedEntries = CompositionStore.desiredEntries(this),
        )
        val state = CompositionState(installed = installed, enabled = enabled)
        val executor = object : CompositionExecutor {
            override fun install(kind: CompositionKind, id: String): Result<Boolean> = when (kind) {
                // 内置插件“安装”只是重新装配，不下载、不经过 materializer。
                CompositionKind.BUILTIN -> runCatching {
                    builtin.setEnabledTransient(id, true).getOrThrow()
                    true
                }
                else -> Result.failure(
                    IllegalStateException("启动期不安装 ${kind.name.lowercase()} 插件"),
                )
            }

            override fun enable(kind: CompositionKind, id: String): Result<Boolean> = when (kind) {
                CompositionKind.UI -> if (uiManager.setEnabled(id, true)) {
                    Result.success(false)
                } else {
                    Result.failure(IllegalStateException("启用失败"))
                }
                CompositionKind.DEX -> runCatching {
                    installer.setEnabled(id, true)
                    false
                }
                CompositionKind.BUILTIN -> builtin.setEnabledTransient(id, true).map { false }
            }

            override fun disable(kind: CompositionKind, id: String): Result<Boolean> = when (kind) {
                CompositionKind.UI -> if (uiManager.setEnabled(id, false)) {
                    Result.success(false)
                } else {
                    Result.failure(IllegalStateException("停用失败"))
                }
                CompositionKind.DEX -> runCatching {
                    installer.setEnabled(id, false)
                    false
                }
                CompositionKind.BUILTIN -> builtin.setEnabledTransient(id, false).map { false }
            }

            override fun restore(
                kind: CompositionKind,
                id: String,
                installed: Boolean,
                enabled: Boolean,
            ): Result<Boolean> = when (kind) {
                CompositionKind.UI -> runCatching {
                    installed && uiManager.setEnabled(id, enabled)
                }
                CompositionKind.DEX -> runCatching {
                    if (installed) {
                        installer.setEnabled(id, enabled)
                        true
                    } else {
                        false
                    }
                }
                CompositionKind.BUILTIN -> runCatching {
                    builtin.setEnabledTransient(id, enabled).getOrThrow()
                    true
                }
            }
        }
        val report = executeStartup(decision, state, executor)
        CompositionStore.recordStartup(
            this,
            decision.plan,
            state,
            StartupOutcome(
                attempted = true,
                canReconcile = decision.canReconcile,
                note = decision.note,
                skipped = decision.diagnosticLines(),
                enabled = report.enabled,
                disabled = report.disabled,
                failures = report.allFailures,
                at = System.currentTimeMillis(),
            ),
        )
        if (report.enabled > 0 || report.disabled > 0) {
            Log.i(
                TAG,
                "startup composition replay: enabled=${report.enabled} disabled=${report.disabled}",
            )
        }
        if (report.allFailures.isNotEmpty()) {
            Log.w(TAG, "startup composition failures: ${report.allFailures.joinToString("；")}")
        }
    }

    private fun handleSSLHandshake() {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate?> {
                    return arrayOfNulls(0)
                }

                override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
            })
            val sc = SSLContext.getInstance("TLS")

            // trustAllCerts信任所有的证书
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (ignored: Exception) {
        }
    }



    /** 供 debug 广播导出内核诊断；同时写 logcat 与 filesDir/diagnostics/。 */
    fun dumpPluginDiagnostics(): String {
        val files = buildDiagnosticsExport()
        val dir = java.io.File(filesDir, "diagnostics").apply { mkdirs() }
        files.forEach { (name, json) -> java.io.File(dir, name).writeText(json) }
        // 旧 debug 脚本仍读 plugins.json：保留只含内核快照的文件。
        java.io.File(dir, "plugins.json").writeText(Gson().toJson(pluginHost.diagnostics()))
        val combined = files.getValue(DIAGNOSTICS_EXPORT_FILE)
        android.util.Log.i(TAG, "plugin diagnostics: $combined")
        return combined
    }

    /**
     * release 诊断导出：生成 `diagnostics.json` 与 `composition.json` 的文本。
     *
     * 不依赖 debug 广播，也不预写应用私有目录——调用方把结果写到用户选择的位置。
     */
    fun buildDiagnosticsExport(): Map<String, String> = pluginDiagnosticsExport(
        diagnostics = pluginHost.diagnostics(),
        composition = runCatching {
            com.stupidtree.hitax.market.CompositionStore.read(this)
                ?.let { JsonParser.parseString(it) as? JsonObject }
        }.getOrNull(),
        exportedAt = System.currentTimeMillis(),
        appVersionName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull(),
    )

    /** 最近一次 applyPreset 的 desired/actual 快照；没有应用过整合包时返回空。 */
    fun dumpCompositionDiagnostics(): String {
        val text = com.stupidtree.hitax.market.CompositionStore.read(this)
            ?: "no composition snapshot; apply a preset first"
        android.util.Log.i(TAG, "composition diagnostics: $text")
        return text
    }

    companion object {
        private const val TAG = "PluginHost"
    }
}

/** release 诊断导出包里 diagnostics 文件名。 */
internal const val DIAGNOSTICS_EXPORT_FILE = "diagnostics.json"

/** release 诊断导出包里 composition 文件名。 */
internal const val COMPOSITION_EXPORT_FILE = "composition.json"

private const val DIAGNOSTICS_EXPORT_SCHEMA = 1

/**
 * 生成 release 诊断导出的两个 JSON 文本。纯函数：不碰文件系统与 Android API，
 * 因此可以在 JVM 单测里确认输出可被 `json.load` 解析。
 *
 * `diagnostics.json` 包含插件快照（含 origin/version/requires/provides）与内嵌的组合快照；
 * `composition.json` 是同一份组合快照的独立副本，没有组合时仍是可解析对象。
 */
internal fun pluginDiagnosticsExport(
    diagnostics: com.stupidtree.hita.kernel.PluginDiagnostics,
    composition: JsonObject?,
    exportedAt: Long,
    appVersionName: String?,
    hostApi: Int = com.stupidtree.hita.api.HITA_API_VERSION,
): Map<String, String> {
    val fallbackComposition = JsonObject().apply {
        addProperty("present", false)
        addProperty("note", "没有已应用的整合包或组合快照")
    }
    val compositionJson = (composition ?: fallbackComposition).toString()
    val diagnosticsJson = JsonObject().apply {
        addProperty("schema", DIAGNOSTICS_EXPORT_SCHEMA)
        addProperty("exportedAt", exportedAt)
        if (appVersionName != null) addProperty("appVersion", appVersionName)
        addProperty("hostApi", hostApi)
        add("plugins", Gson().toJsonTree(diagnostics))
        add("composition", JsonParser.parseString(compositionJson))
    }.toString()
    return mapOf(
        DIAGNOSTICS_EXPORT_FILE to diagnosticsJson,
        COMPOSITION_EXPORT_FILE to compositionJson,
    )
}
