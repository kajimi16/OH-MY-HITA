package com.stupidtree.hitax.data.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.ExamService
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.R
import com.stupidtree.hitax.data.model.eas.ExamItem
import com.stupidtree.hitax.data.source.preference.ExamReminderStore
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 考试提醒 Worker。
 *
 * 与 iOS `fixedExamReminderDates` 对齐：考前 24h / 12h / 1h / 30m 四个固定档位。
 * 数据来自教务插件提供的 [ExamService]，考试时间由 `examDate`（yyyy-MM-dd）与
 * `examTime`（HH:mm-HH:mm）拼出。插件缺席或查询超时都视为本轮失败，
 * 只排下一次检查，不阻塞、不崩溃。
 */
class ExamReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : Worker(appContext, params) {

    override fun doWork(): Result {
        if (!ExamReminderPolicy.isEnabled(applicationContext)) {
            ExamReminderScheduler.cancel(applicationContext)
            return Result.success()
        }
        val store = ExamReminderStore.getInstance(applicationContext)
        if (!store.isEnabled()) return Result.success()

        val app = applicationContext as? HApplication ?: return Result.success()
        val service = app.pluginHost.service(ExamService.KEY)
        if (service == null) {
            ExamReminderScheduler.scheduleNextCheck(applicationContext, MISSING_SERVICE_RECHECK_MS)
            return Result.success()
        }

        val now = System.currentTimeMillis()
        val fetched = awaitExams(service.exams(), FETCH_TIMEOUT_MS)
        if (fetched == null || fetched.state != DataState.STATE.SUCCESS) {
            // 网络失败/未登录/超时：保持排程链，稍后重试。
            ExamReminderScheduler.scheduleNextCheck(applicationContext, FETCH_RETRY_DELAY_MS)
            return Result.success()
        }

        val exams = fetched.data.orEmpty()
            .mapNotNull { it.toScheduleExam() }
            .filter { it.startMillis in (now + 1)..(now + LOOKAHEAD_MS) }

        val sent = store.getSentKeys().toMutableSet()
        val due = ExamReminderSchedule.dueNow(exams, now, LEADS, sent)
        val sentNow = mutableSetOf<String>()
        for (trigger in due) {
            val exam = exams.firstOrNull { it.id == trigger.examId } ?: continue
            ExamReminderNotifier.notify(applicationContext, exam, trigger)
            sentNow += trigger.key
        }
        if (sentNow.isNotEmpty()) {
            store.markSent(sentNow)
            sent += sentNow
        }

        val next = ExamReminderSchedule.nextDue(exams, now, LEADS, sent)
        val delay = next
            ?.let {
                (it.fireAtMillis - now)
                    .coerceIn(MIN_RECHECK_DELAY_MS, MAX_RECHECK_DELAY_MS)
            }
            ?: MAX_RECHECK_DELAY_MS
        ExamReminderScheduler.scheduleNextCheck(applicationContext, delay)
        return Result.success()
    }

    /**
     * 在 Worker 线程等待 LiveData 的首个终态。
     *
     * `observeForever` 与 `removeObserver` 必须主线程执行，所以用 Handler 投递；
     * 超时返回 null，由调用方按失败处理。
     */
    private fun awaitExams(
        liveData: LiveData<DataState<List<ExamItem>>>,
        timeoutMs: Long,
    ): DataState<List<ExamItem>>? {
        val latch = CountDownLatch(1)
        var result: DataState<List<ExamItem>>? = null
        val observer = Observer<DataState<List<ExamItem>>> { state ->
            if (state.state != DataState.STATE.NOTHING) {
                result = state
                latch.countDown()
            }
        }
        val handler = Handler(Looper.getMainLooper())
        handler.post { liveData.observeForever(observer) }
        try {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        } finally {
            handler.post { liveData.removeObserver(observer) }
        }
        return result
    }

    private fun ExamItem.toScheduleExam(): ExamReminderSchedule.Exam? {
        val key = ExamReminderSchedule.examKey(courseName, examDate, examTime) ?: return null
        val start = ExamReminderSchedule.startMillis(examDate, examTime) ?: return null
        return ExamReminderSchedule.Exam(
            id = key,
            name = courseName?.trim().orEmpty(),
            place = examLocation?.trim().orEmpty(),
            startMillis = start,
        )
    }

    companion object {
        /** 与 iOS `fixedExamReminderDates` 一致：24h / 12h / 1h / 30m。 */
        private val LEADS = ExamReminderSchedule.DEFAULT_LEADS_MINUTES

        private const val LOOKAHEAD_MS = 30L * 24L * 60L * 60L * 1000L
        private const val FETCH_TIMEOUT_MS = 20_000L
        private const val FETCH_RETRY_DELAY_MS = 30L * 60L * 1000L
        private const val MISSING_SERVICE_RECHECK_MS = 6L * 60L * 60L * 1000L
        private const val MIN_RECHECK_DELAY_MS = 5_000L
        /** 最长 6 小时重新检查一次：用户随时可能新增考试或修改系统时间。 */
        private const val MAX_RECHECK_DELAY_MS = 6L * 60L * 60L * 1000L
    }
}
