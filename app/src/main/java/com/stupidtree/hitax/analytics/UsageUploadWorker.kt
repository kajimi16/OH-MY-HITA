package com.stupidtree.hitax.analytics

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.stupidtree.hitax.data.source.preference.UsageStatsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 批量上传匿名事件到自建 HITA 后端。
 * 服务端契约见 docs/api/usage.md；缺失/失败时保留队列等待重试。
 */
class UsageUploadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext
        if (!UsageStatsStore.isEnabled(app)) return@withContext Result.success()
        val dao = UsageDatabase.get(app).usageDao()
        val rows = dao.pending(100)
        if (rows.isEmpty()) return@withContext Result.success()
        val payload = JSONObject().apply {
            put("events", JSONArray().apply {
                rows.forEach { e ->
                    put(JSONObject().apply {
                        put("install_id", e.installId)
                        put("event", e.event)
                        put("screen", e.screen ?: JSONObject.NULL)
                        put("plugin_id", e.pluginId ?: JSONObject.NULL)
                        put("app_version", e.appVersion)
                        put("sdk", e.sdk)
                        put("timestamp", e.timestamp)
                    })
                }
            })
        }
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "OH-MY-HITA-Usage")
        }
        try {
            connection.outputStream.use { it.write(payload.toString().toByteArray()) }
            if (connection.responseCode in 200..299) {
                dao.delete(rows.map { it.id })
                Result.success()
            } else {
                Result.retry()
            }
        } catch (_: Exception) {
            Result.retry()
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private val ENDPOINT get() = com.stupidtree.hitax.BuildConfig.USAGE_BASE_URL.trimEnd('/') + "/api/usage"
    }
}
