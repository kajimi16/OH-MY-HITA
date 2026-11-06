package com.stupidtree.hitax.livecourse

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.stupidtree.hitax.R
import com.stupidtree.hitax.ui.main.MainActivity

/** 直播课前台守护：通知 + 打开链接；课程结束后自动停止。 */
class LiveCourseGuardService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var stopAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val owner = intent?.getStringExtra(LiveCourseAlarm.EXTRA_OWNER)
        val id = intent?.getStringExtra(LiveCourseAlarm.EXTRA_ID)
        if (owner == null || id == null) { stopSelf(); return START_NOT_STICKY }
        val spec = LiveCourseStore(this).all()
            .firstOrNull { it.ownerId == owner && it.id == id }
        if (spec == null) { stopSelf(); return START_NOT_STICKY }

        createChannel()
        val open = Intent(Intent.ACTION_VIEW, Uri.parse(spec.url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val openPending = PendingIntent.getActivity(
            this, spec.id.hashCode(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val content = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_nav_today)
            .setContentTitle("直播课：${spec.title}")
            .setContentText("课程进行中，点按打开直播")
            .setContentIntent(openPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "打开直播", openPending)
            .build()

        startForeground(spec.id.hashCode(), notification)
        stopAt = spec.endAtMillis
        val delay = (stopAt - System.currentTimeMillis()).coerceAtLeast(10_000)
        handler.postDelayed({ stopSelf() }, delay)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(
                    NotificationChannel(CHANNEL, "直播课", NotificationManager.IMPORTANCE_HIGH)
                )
        }
    }

    companion object {
        private const val CHANNEL = "live_course"
    }
}
