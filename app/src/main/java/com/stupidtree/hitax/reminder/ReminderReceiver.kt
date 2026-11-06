package com.stupidtree.hitax.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.stupidtree.hitax.R
import com.stupidtree.hitax.ui.main.MainActivity

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_REMIND) return
        val ownerId = intent.getStringExtra(ReminderScheduler.EXTRA_OWNER) ?: return
        val id = intent.getStringExtra(ReminderScheduler.EXTRA_ID) ?: return

        val store = ReminderStore(context)
        val reminder = store.all().firstOrNull { it.ownerId == ownerId && it.id == id } ?: return
        store.remove(ownerId, id)
        notify(context, reminder)
    }

    private fun notify(context: Context, reminder: StoredReminder) {
        if (!hasNotificationPermission(context)) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val contentIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_today)
            .setContentTitle(reminder.title)
            .setContentText(reminder.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.text))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(reminder.id.hashCode(), notification)
        }
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "插件提醒",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "由市场插件安排的上课、考试等提醒"
                    enableVibration(true)
                }
            )
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    companion object {
        const val CHANNEL_ID = "plugin_reminders"
    }
}
