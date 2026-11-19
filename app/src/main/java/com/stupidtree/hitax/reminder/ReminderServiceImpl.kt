package com.stupidtree.hitax.reminder

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.stupidtree.hita.api.ReminderService
import com.stupidtree.hita.api.ReminderSpec

internal class ReminderServiceImpl(context: Context) : ReminderService {

    private val appContext = context.applicationContext
    private val store = ReminderStore(appContext)

    override fun canScheduleExactAlarms(): Boolean =
        ReminderScheduler.canScheduleExactAlarms(appContext)

    override fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return appContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
        return NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    }

    override fun schedule(spec: ReminderSpec): Boolean {
        if (ReminderRules.validate(spec, System.currentTimeMillis()) != null) return false
        if (!canPostNotifications()) return false
        store.upsert(spec)
        if (!ReminderScheduler.schedule(appContext, spec)) {
            store.remove(spec.ownerId, spec.id)
            return false
        }
        return true
    }

    override fun cancel(ownerId: String, id: String) {
        ReminderScheduler.cancel(appContext, ownerId, id)
        store.remove(ownerId, id)
    }

    override fun cancelAll(ownerId: String) {
        store.all().filter { it.ownerId == ownerId }.forEach {
            ReminderScheduler.cancel(appContext, ownerId, it.id)
        }
        store.removeAll(ownerId)
    }
}
