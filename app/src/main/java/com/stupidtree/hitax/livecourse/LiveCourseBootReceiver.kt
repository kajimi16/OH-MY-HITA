package com.stupidtree.hitax.livecourse

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LiveCourseBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val store = LiveCourseStore(context)
        val now = System.currentTimeMillis()
        store.all().forEach { spec ->
            if (spec.endAtMillis > now) LiveCourseAlarm.schedule(context, spec)
            else store.remove(spec.ownerId, spec.id)
        }
    }
}
