package com.stupidtree.hitax.livecourse

import android.content.Context
import com.stupidtree.hita.api.LiveCourseService
import com.stupidtree.hita.api.LiveCourseSpec

internal class LiveCourseServiceImpl(context: Context) : LiveCourseService {
    private val appContext = context.applicationContext
    private val store = LiveCourseStore(appContext)

    override fun schedule(spec: LiveCourseSpec): Boolean {
        if (spec.ownerId.isBlank() || spec.id.isBlank() || spec.title.isBlank()) return false
        if (spec.endAtMillis <= System.currentTimeMillis()) return false
        store.upsert(spec)
        if (!LiveCourseAlarm.schedule(appContext, spec)) {
            store.remove(spec.ownerId, spec.id)
            return false
        }
        return true
    }

    override fun cancel(ownerId: String, id: String) {
        LiveCourseAlarm.cancel(appContext, ownerId, id)
        store.remove(ownerId, id)
    }

    override fun cancelAll(ownerId: String) {
        store.all().filter { it.ownerId == ownerId }.forEach {
            LiveCourseAlarm.cancel(appContext, ownerId, it.id)
        }
        store.removeAll(ownerId)
    }
}
