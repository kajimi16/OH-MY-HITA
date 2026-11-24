package com.stupidtree.hitax.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.Observer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.data.work.CourseReminderWorker
import java.sql.Timestamp
import java.util.UUID

/** Debug-only fixture: insert a CLASS event ending in 5 minutes, then run the reminder worker. */
class DevClassEndingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as HApplication
        val service = app.pluginHost.service(TimetableService.KEY)
        if (service == null) {
            Log.e(TAG, "TimetableService missing")
            return
        }
        val pending = goAsync()
        val live = service.getRecentTimetable()
        val observer = object : Observer<Timetable?> {
            override fun onChanged(timetable: Timetable?) {
                if (timetable == null) return
                live.removeObserver(this)
                Thread {
                    runCatching {
                        val now = System.currentTimeMillis()
                        val event = EventItem().apply {
                            id = UUID.randomUUID().toString()
                            type = EventItem.TYPE.CLASS
                            name = "下课提醒验收"
                            place = "A101"
                            this.timetableId = timetable.id
                            from = Timestamp(now - 55L * 60 * 1000)
                            to = Timestamp(now + 10L * 60 * 1000)
                        }
                        service.saveEvents(listOf(event))
                        WorkManager.getInstance(context)
                            .enqueue(OneTimeWorkRequestBuilder<CourseReminderWorker>().build())
                        Log.i(TAG, "inserted ${event.id} and enqueued worker")
                    }.onFailure { Log.e(TAG, "fixture failed", it) }
                    pending.finish()
                }.start()
            }
        }
        live.observeForever(observer)
    }

    private companion object {
        const val TAG = "DevClassEnding"
    }
}
