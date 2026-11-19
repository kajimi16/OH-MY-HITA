package com.stupidtree.hitax.background

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.stupidtree.hita.api.BackgroundTaskService
import java.util.concurrent.TimeUnit

internal class BackgroundTaskServiceImpl(context: Context) : BackgroundTaskService {

    private val appContext = context.applicationContext

    override fun scheduleOnce(ownerId: String, taskId: String, delayMillis: Long): Boolean {
        if (ownerId.isBlank() || taskId.isBlank() || delayMillis <= 0) return false
        val request = OneTimeWorkRequestBuilder<PluginTaskWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data(ownerId, taskId))
            .addTag(tag(ownerId))
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            name(ownerId, taskId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
        return true
    }

    override fun schedulePeriodic(
        ownerId: String,
        taskId: String,
        intervalMinutes: Int,
    ): Boolean {
        if (ownerId.isBlank() || taskId.isBlank() || intervalMinutes < 15) return false
        val request = PeriodicWorkRequestBuilder<PluginTaskWorker>(
            intervalMinutes.toLong(), TimeUnit.MINUTES,
        )
            .setInputData(data(ownerId, taskId))
            .addTag(tag(ownerId))
            .build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            name(ownerId, taskId),
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
        return true
    }

    override fun cancel(ownerId: String, taskId: String) {
        WorkManager.getInstance(appContext).cancelUniqueWork(name(ownerId, taskId))
    }

    override fun cancelAll(ownerId: String) {
        WorkManager.getInstance(appContext).cancelAllWorkByTag(tag(ownerId))
    }

    private fun data(ownerId: String, taskId: String): Data =
        Data.Builder()
            .putString(PluginTaskWorker.KEY_OWNER, ownerId)
            .putString(PluginTaskWorker.KEY_TASK, taskId)
            .build()

    private fun name(ownerId: String, taskId: String) = "plugin-task:$ownerId:$taskId"
    private fun tag(ownerId: String) = "plugin-owner:$ownerId"
}
