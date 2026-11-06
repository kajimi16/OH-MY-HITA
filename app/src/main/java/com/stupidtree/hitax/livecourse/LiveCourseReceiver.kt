package com.stupidtree.hitax.livecourse

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class LiveCourseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != LiveCourseAlarm.ACTION) return
        val owner = intent.getStringExtra(LiveCourseAlarm.EXTRA_OWNER) ?: return
        val id = intent.getStringExtra(LiveCourseAlarm.EXTRA_ID) ?: return
        val spec = LiveCourseStore(context).all()
            .firstOrNull { it.ownerId == owner && it.id == id } ?: return
        ContextCompat.startForegroundService(
            context,
            Intent(context, LiveCourseGuardService::class.java).apply {
                putExtra(LiveCourseAlarm.EXTRA_OWNER, owner)
                putExtra(LiveCourseAlarm.EXTRA_ID, id)
            },
        )
    }
}
