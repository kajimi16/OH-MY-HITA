package com.stupidtree.hitax.data.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.stupidtree.hitax.R

/**
 * 考试提醒的通知投递；Worker 与 debug fixture 共用同一实现。
 */
object ExamReminderNotifier {

    fun notify(context: Context, exam: ExamReminderSchedule.Exam, trigger: ExamReminderSchedule.Trigger) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        // 不直接引用 MainActivity：data 层依赖 ui 层会被分层检查拦下。
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
            ?: return
        val requestCode = trigger.key.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val text = buildString {
            append(exam.name)
            if (exam.place.isNotBlank()) append(" @ ${exam.place}")
            append("，${leadLabel(trigger.leadMinutes)}后开考")
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_today)
            .setContentTitle("考试提醒")
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        manager.notify(requestCode, notification)
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "考试提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "在考试开始前提醒"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            },
        )
    }

    private fun leadLabel(minutes: Int): String = when {
        minutes % (24 * 60) == 0 -> "${minutes / (24 * 60)} 天"
        minutes % 60 == 0 -> "${minutes / 60} 小时"
        else -> "$minutes 分钟"
    }

    private const val CHANNEL_ID = "exam_reminder"
}
