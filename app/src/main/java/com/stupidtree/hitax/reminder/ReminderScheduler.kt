package com.stupidtree.hitax.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.stupidtree.hita.api.ReminderSpec

/** AlarmManager 调度；不持久化，持久化由 [ReminderStore] 负责。 */
internal object ReminderScheduler {

    const val ACTION_REMIND = "com.stupidtree.hitax.reminder.REMIND"
    const val EXTRA_OWNER = "owner"
    const val EXTRA_ID = "id"

    fun schedule(context: Context, spec: ReminderSpec): Boolean = runCatching {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, spec.ownerId, spec.id)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        when {
            exact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, spec.triggerAtMillis, pi)
            exact ->
                am.setExact(AlarmManager.RTC_WAKEUP, spec.triggerAtMillis, pi)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, spec.triggerAtMillis, pi)
            else ->
                am.set(AlarmManager.RTC_WAKEUP, spec.triggerAtMillis, pi)
        }
        true
    }.getOrElse { false }

    fun cancel(context: Context, ownerId: String, id: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context, ownerId, id))
    }

    fun canScheduleExactAlarms(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()

    private fun pendingIntent(context: Context, ownerId: String, id: String): PendingIntent {
        val uri = Uri.Builder()
            .scheme("hita-reminder")
            .authority(context.packageName)
            .appendPath(ownerId)
            .appendPath(id)
            .build()
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            data = uri
            putExtra(EXTRA_OWNER, ownerId)
            putExtra(EXTRA_ID, id)
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
