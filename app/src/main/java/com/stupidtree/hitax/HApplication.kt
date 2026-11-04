package com.stupidtree.hitax

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import android.util.Log
import androidx.annotation.WorkerThread
import com.google.gson.JsonObject
import com.stupidtree.stupiduser.data.repository.LocalUserRepository
import com.stupidtree.sync.StupidSync
import com.stupidtree.hita.api.EasService
import com.stupidtree.hita.api.HostUiService
import com.stupidtree.hita.api.ThemeController
import com.stupidtree.hita.kernel.PluginHost
import com.stupidtree.hitax.plugin.PluginCatalog
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
        })
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
                        .findByCapability(capability) != null

                override fun open(
                    context: android.content.Context,
                    capability: String,
                    arguments: android.os.Bundle,
                ): Boolean {
                    val record = com.stupidtree.hitax.uiplugin.UiPluginManager(this@HApplication)
                        .findByCapability(capability) ?: return false
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
        pluginHost.installAll(PluginCatalog.plugins(this))
        // 组件开关状态由系统持久化，可能与本地禁用标记不一致（如清除应用数据）；
        // 装配完成后按标记收敛一次，避免“插件已装配但组件仍禁用”。
        com.stupidtree.hitax.plugin.PluginCatalog.entries(this).forEach { entry ->
            com.stupidtree.hitax.plugin.PluginComponents.setEnabled(
                this, entry.id, !com.stupidtree.hitax.plugin.PluginPrefs.isDisabled(this, entry.id)
            )
        }
        // 恢复上次安装的运行时插件（T1）；单个失败不影响其它插件。
        com.stupidtree.hitax.market.PluginInstaller(this, pluginHost).restore()

        // 宿主不再认识任何具体数据表：只按 key 查 SyncCodec 并转发。
        StupidSync.init(this, object : StupidSync.PushDelegate {

            @WorkerThread
            override fun getDataForIds(key: String, ids: List<String>): List<JsonObject> =
                pluginHost.sync.get(key)?.export(ids).orEmpty()

            @WorkerThread
            override fun saveData(key: String, ids: List<String>, data: List<String>) {
                pluginHost.sync.get(key)?.save(ids, data)
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



    companion object {
        private const val TAG = "PluginHost"
    }
}
