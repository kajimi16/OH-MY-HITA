package com.stupidtree.hitax.livecourse

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.stupidtree.hita.api.LiveCourseSpec

internal object LiveCourseAlarm {
    const val ACTION = "com.stupidtree.hitax.livecourse.START"
    const val EXTRA_OWNER = "owner"
    const val EXTRA_ID = "id"

    fun triggerAt(spec: LiveCourseSpec): Long =
        maxOf(spec.startAtMillis - spec.advanceMinutes * 60_000L, System.currentTimeMillis() + 2_000)

    fun schedule(context: Context, spec: LiveCourseSpec): Boolean = runCatching {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(context, spec.ownerId, spec.id)
        val time = triggerAt(spec)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        when {
            exact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
            exact -> am.setExact(AlarmManager.RTC_WAKEUP, time, pi)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
            else -> am.set(AlarmManager.RTC_WAKEUP, time, pi)
        }
        true
    }.getOrDefault(false)

    fun cancel(context: Context, ownerId: String, id: String) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .cancel(pending(context, ownerId, id))
    }

    private fun pending(context: Context, ownerId: String, id: String): PendingIntent {
        val intent = Intent(context, LiveCourseReceiver::class.java).apply {
            action = ACTION
            data = Uri.Builder().scheme("hita-live").authority(context.packageName)
                .appendPath(ownerId).appendPath(id).build()
            putExtra(EXTRA_OWNER, ownerId)
            putExtra(EXTRA_ID, id)
        }
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
