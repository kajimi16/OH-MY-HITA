package com.stupidtree.hitax.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hitax.HApplication

/**
 * Debug-only fixture seam. Imports a bundled ICS through the host service so
 * screenshot runs can exercise fixed-data states without touching user files.
 */
class DevLiteSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as HApplication
        val service = app.pluginHost.service(TimetableService.KEY)
        if (service == null) {
            Log.e(TAG, "TimetableService missing")
            return
        }
        val pending = goAsync()
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            runCatching {
                val bytes = context.assets.open(ASSET).use { it.readBytes() }
                service.importFromICSAsNewTimetable(
                    java.io.ByteArrayInputStream(bytes),
                    "fixture-week12",
                ).observeForever { state ->
                    Log.i(TAG, "import state=${state.state} data=${state.data}")
                    pending.finish()
                }
            }.onFailure {
                Log.e(TAG, "import failed", it)
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "DevLiteSeed"
        const val ASSET = "fixtures_week12_multi.ics"
    }
}
