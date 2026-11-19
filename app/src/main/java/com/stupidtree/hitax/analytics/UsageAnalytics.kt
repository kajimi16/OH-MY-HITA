package com.stupidtree.hitax.analytics

import android.content.Context
import android.os.Build
import com.stupidtree.hitax.data.source.preference.UsageStatsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 匿名使用统计。
 *
 * 默认关闭；只允许白名单事件，且只包含匿名安装 id、版本、事件、页面/插件 id、时间。
 * 绝不上报账号、学号、姓名、课程内容或自由文本。
 */
object UsageAnalytics {
    private const val PREFS = "usage_analytics"
    private const val KEY_INSTALL_ID = "install_id"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun record(
        context: Context,
        event: String,
        screen: String? = null,
        pluginId: String? = null,
    ) {
        if (!UsageStatsStore.isEnabled(context)) return
        if (event !in UsageEventAllowlist.all) return
        val app = context.applicationContext
        val installId = installId(app)
        val version = runCatching {
            app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "0.0.1"
        }.getOrDefault("0.0.1")
        scope.launch {
            UsageDatabase.get(app).usageDao().insert(
                UsageEvent(
                    installId = installId,
                    event = event,
                    screen = screen,
                    pluginId = pluginId,
                    appVersion = version,
                    sdk = Build.VERSION.SDK_INT,
                    timestamp = System.currentTimeMillis(),
                )
            )
            val pending = UsageDatabase.get(app).usageDao().count()
            UsageUploadScheduler.enqueue(app, immediate = pending >= 20)
        }
    }

    fun clear(context: Context) {
        val app = context.applicationContext
        scope.launch {
            UsageDatabase.get(app).usageDao().clear()
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .remove(KEY_INSTALL_ID).apply()
        }
    }

    fun installId(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_INSTALL_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_INSTALL_ID, it).apply()
        }
    }
}
