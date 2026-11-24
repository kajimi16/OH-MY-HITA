package com.stupidtree.hitax.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.stupidtree.hitax.data.source.preference.ExamReminderStore
import java.util.concurrent.TimeUnit

/**
 * 考试提醒调度器。
 *
 * 一次性任务自调度链：每次检查后按 [ExamReminderSchedule.nextDue] 排下一次。
 * 使用独立唯一名，避免和课程提醒互相替换掉对方的在途检查。
 */
object ExamReminderScheduler {

    private const val UNIQUE_NEXT_WORK_NAME = "exam_reminder_next"

    fun ensureScheduled(context: Context) {
        if (!ExamReminderPolicy.isEnabled(context)) {
            cancel(context)
            return
        }
        enqueue(context, 0L, ExistingWorkPolicy.KEEP)
    }

    fun scheduleNextCheck(context: Context, delayMs: Long) {
        enqueue(context, delayMs, ExistingWorkPolicy.REPLACE)
    }

    private fun enqueue(context: Context, delayMs: Long, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<ExamReminderWorker>()
            .setInitialDelay(delayMs.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_NEXT_WORK_NAME,
            policy,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NEXT_WORK_NAME)
    }

    /** 跟随课程提醒总开关。 */
    fun autoSchedule(context: Context) {
        if (ExamReminderPolicy.isEnabled(context) &&
            ExamReminderStore.getInstance(context).isEnabled()
        ) {
            ensureScheduled(context)
        } else {
            cancel(context)
        }
    }
}
