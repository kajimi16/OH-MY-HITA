package com.stupidtree.hitax

import android.app.Application
import android.util.Log
import androidx.annotation.WorkerThread
import com.google.gson.JsonObject
import com.stupidtree.stupiduser.data.repository.LocalUserRepository
import com.stupidtree.sync.StupidSync
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

    /** 微内核。插件只通过它注册服务与导航项。 */
    lateinit var pluginHost: PluginHost
        private set

    override fun onCreate() {
        super.onCreate()
        // 万物皆插件：先装配内核，再初始化内核自身依赖的业务。
        pluginHost = PluginHost(
            pluginsDir = File(filesDir, "plugins"),
            logger = { message, error ->
                if (error == null) Log.i(TAG, message) else Log.e(TAG, message, error)
            },
        )
        pluginHost.installAll(PluginCatalog.plugins(this))
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
