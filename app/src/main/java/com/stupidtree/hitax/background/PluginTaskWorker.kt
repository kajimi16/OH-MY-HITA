package com.stupidtree.hitax.background

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.stupidtree.hita.api.BackgroundTask
import com.stupidtree.hita.api.BackgroundTaskHost
import com.stupidtree.hita.api.ServiceKey
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.uiplugin.UiPluginManager

/** 执行插件注册的后台任务；插件缺失或停用时静默跳过。 */
class PluginTaskWorker(
    appContext: Context,
    params: WorkerParameters,
) : Worker(appContext, params) {

    override fun doWork(): Result {
        val ownerId = inputData.getString(KEY_OWNER) ?: return Result.success()
        val taskId = inputData.getString(KEY_TASK) ?: return Result.success()
        val manager = UiPluginManager(applicationContext)
        val record = manager.installed().firstOrNull { it.id == ownerId }
        if (record == null || !record.enabled) {
            Log.i(TAG, "skip background task $ownerId/$taskId: plugin unavailable")
            return Result.success()
        }
        return runCatching {
            val plugin = manager.instantiate(record)
            check(plugin is BackgroundTask) { "插件未实现 BackgroundTask" }
            plugin.run(applicationContext, Host(applicationContext))
        }.fold(
            onSuccess = { Result.success() },
            onFailure = {
                Log.w(TAG, "background task failed $ownerId/$taskId", it)
                Result.failure()
            },
        )
    }

    private class Host(private val context: Context) : BackgroundTaskHost {
        override fun <T : Any> service(key: ServiceKey<T>): T? =
            (context.applicationContext as HApplication).pluginHost.service(key)
    }

    companion object {
        const val KEY_OWNER = "owner"
        const val KEY_TASK = "task"
        private const val TAG = "PluginTaskWorker"
    }
}
