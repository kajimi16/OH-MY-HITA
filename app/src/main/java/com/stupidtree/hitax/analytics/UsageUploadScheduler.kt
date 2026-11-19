package com.stupidtree.hitax.analytics

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object UsageUploadScheduler {
    private const val IMMEDIATE_WORK = "usage_upload_now"
    private const val PERIODIC_WORK = "usage_upload_periodic"

    /** 每 6 小时批量上传；队列达到 20 条时额外立即触发一次。 */
    fun enqueue(context: Context, immediate: Boolean = false) {
        val app = context.applicationContext
        val manager = WorkManager.getInstance(app)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        if (immediate) {
            val request = OneTimeWorkRequestBuilder<UsageUploadWorker>()
                .setConstraints(constraints)
                .setInitialDelay(15, TimeUnit.SECONDS)
                .build()
            manager.enqueueUniqueWork(IMMEDIATE_WORK, ExistingWorkPolicy.KEEP, request)
        }
        val periodic = PeriodicWorkRequestBuilder<UsageUploadWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        manager.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, periodic)
    }
}
