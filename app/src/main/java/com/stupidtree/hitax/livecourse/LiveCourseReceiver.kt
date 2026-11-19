package com.stupidtree.hitax.livecourse

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class LiveCourseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != LiveCourseAlarm.ACTION) return
        // 全局开关关闭时不拉起守护服务（对齐 3.1.2「更多」页的直播课开关）
        if (!LiveCourseSettings.isEnabled(context)) return
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
