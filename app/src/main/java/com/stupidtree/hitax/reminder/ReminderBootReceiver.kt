package com.stupidtree.hitax.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 重启/覆盖安装后恢复未来提醒；已过期的提醒直接丢弃。 */
class ReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> Unit
            else -> return
        }
        val store = ReminderStore(context)
        val now = System.currentTimeMillis()
        store.all().forEach { reminder ->
            if (reminder.triggerAtMillis > now &&
                ReminderScheduler.schedule(context, reminder.toSpec())
            ) {
                // still scheduled
            } else {
                store.remove(reminder.ownerId, reminder.id)
            }
        }
    }
}
