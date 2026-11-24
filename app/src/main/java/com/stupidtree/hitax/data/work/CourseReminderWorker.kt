package com.stupidtree.hitax.data.work

import com.stupidtree.hitax.HitaServices
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.stupidtree.hitax.R
import com.stupidtree.hitax.data.source.preference.CourseReminderStore
import com.stupidtree.hitax.ui.main.MainActivity
import com.stupidtree.hitax.utils.TimeTools
import java.util.Calendar

/**
 * 课程提醒 Worker
 * 每15分钟检查一次是否有即将开始的课程
 */
class CourseReminderWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {

    override fun doWork(): Result {
        val store = CourseReminderStore.getInstance(applicationContext)
        val startEnabled = store.isEnabled()
        val endEnabled = store.isEndReminderEnabled()
        if (!startEnabled && !endEnabled) return Result.success()

        val minutes = store.getReminderMinutes()
        val app = applicationContext as? Application ?: return Result.success()
        val timetableRepo = HitaServices.timetable(app)

        val now = System.currentTimeMillis()
        val checkEnd = now + 15L * 60 * 1000

        // 获取今天的事件
        val todayEvents = timetableRepo.getTodayEventsSync()

        // 获取已发送提醒的课程 ID（使用 SharedPreferences 简单存储）
        val sentReminders = getSentReminders()
        val newEvents = if (startEnabled) {
            CourseReminderSchedule.startDue(todayEvents, now, minutes, sentReminders)
        } else {
            emptyList()
        }

        val endingEvents = if (endEnabled) {
            CourseReminderSchedule.endDue(
                events = todayEvents,
                now = now,
                leadMinutes = store.getEndReminderMinutes(),
                windowEnd = checkEnd,
                sent = sentReminders,
            )
        } else {
            emptyList()
        }

        if (newEvents.isNotEmpty() || endingEvents.isNotEmpty()) {
            newEvents.forEach { sendNotification(it) }
            endingEvents.forEach { sendEndNotification(it, store.getEndReminderMinutes()) }
            saveSentReminders(
                sentReminders +
                    CourseReminderSchedule.startKeys(newEvents) +
                    CourseReminderSchedule.endKeys(endingEvents),
            )
        }

        return Result.success()
    }

    private fun sendNotification(event: com.stupidtree.hitax.data.model.timetable.EventItem) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val store = CourseReminderStore.getInstance(applicationContext)
        val minutes = store.getReminderMinutes()

        val title = "⏰ 即将上课"
        val content = buildString {
            append("${event.name}")
            if (!event.place.isNullOrBlank()) {
                append(" @ ${event.place}")
            }
            append(" 将在 ${minutes}分钟后开始")
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_today)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        manager.notify(event.id.hashCode(), notification)
    }

    private fun sendEndNotification(
        event: com.stupidtree.hitax.data.model.timetable.EventItem,
        minutes: Int,
    ) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val requestCode = ("end:" + event.id).hashCode()
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val content = buildString {
            append(event.name)
            if (!event.place.isNullOrBlank()) append(" @ ${event.place}")
            append(" 将在 ${minutes}分钟后结束")
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_today)
            .setContentTitle("⏰ 即将下课")
            .setContentText(content)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        manager.notify(requestCode, notification)
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "课程提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "在课程开始前或结束前提醒"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun getSentReminders(): Set<String> {
        val prefs = applicationContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val today = TimeTools.getDateString(Calendar.getInstance())
        val savedDate = prefs.getString(KEY_DATE, "")
        
        // 如果是新的一天，清空记录
        return if (savedDate != today) {
            emptySet()
        } else {
            prefs.getStringSet(KEY_SENT_IDS, emptySet()) ?: emptySet()
        }
    }

    private fun saveSentReminders(ids: Set<String>) {
        val prefs = applicationContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val today = TimeTools.getDateString(Calendar.getInstance())
        prefs.edit()
            .putStringSet(KEY_SENT_IDS, ids)
            .putString(KEY_DATE, today)
            .apply()
    }

    companion object {
        private const val CHANNEL_ID = "course_reminder"
        private const val SP_NAME = "course_reminder_sent"
        private const val KEY_SENT_IDS = "sent_ids"
        private const val KEY_DATE = "date"
    }
}
