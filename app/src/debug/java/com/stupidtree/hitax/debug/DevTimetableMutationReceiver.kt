package com.stupidtree.hitax.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.Observer
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.Timetable

/**
 * Debug-only timetable mutation used by version-history screenshot/behavior runs.
 * It changes exactly one CLASS event name through the host service.
 */
class DevTimetableMutationReceiver : BroadcastReceiver() {
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
                        val events = service.getEventsDuringSync(
                            timetable.startTime.time,
                            timetable.endTime.time.coerceAtLeast(
                                timetable.startTime.time + 20L * 7 * 24 * 3600 * 1000,
                            ),
                        ).filter {
                            it.type == EventItem.TYPE.CLASS && it.timetableId == timetable.id
                        }
                        val target = events.firstOrNull()
                        if (target == null) {
                            Log.e(TAG, "No class event to mutate")
                        } else {
                            target.name = target.name + "·改"
                            service.saveEvents(listOf(target))
                            Log.i(TAG, "mutated ${target.id} -> ${target.name}")
                        }
                    }.onFailure { Log.e(TAG, "mutation failed", it) }
                    pending.finish()
                }.start()
            }
        }
        live.observeForever(observer)
    }

    private companion object {
        const val TAG = "DevTimetableMutation"
    }
}
