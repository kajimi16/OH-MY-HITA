package com.stupidtree.hitax.shell

import android.content.Context
import com.stupidtree.hitax.data.model.timetable.EventItem
import org.json.JSONObject

/** Pure timetable snapshot/diff logic used by the shell change pill. */
internal object ShellTimetableChange {

    /** Stable per-course signature; excludes id, color and createdAt. */
    fun signature(event: EventItem): String = listOf(
        event.type.name,
        event.name,
        event.subjectId,
        event.from.time,
        event.to.time,
        event.place.orEmpty(),
        event.teacher.orEmpty(),
    ).joinToString("\u001f")

    /** Counts added, removed and field-modified courses once per event id. */
    fun diff(previous: Map<String, String>, current: Map<String, String>): Int {
        val ids = previous.keys + current.keys
        return ids.count { id -> previous[id] != current[id] }
    }

    fun snapshot(events: List<EventItem>): Map<String, String> =
        events.associate { it.id to signature(it) }
}

/** Persists the last seen snapshot per timetable/week so a later launch can detect changes. */
internal class ShellTimetableSnapshotStore(context: Context) {
    private val prefs = context.getSharedPreferences("shell_timetable_snapshots", Context.MODE_PRIVATE)

    fun load(key: String): Map<String, String>? {
        val raw = prefs.getString(key, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { id -> put(id, json.getString(id)) }
            }
        }.getOrNull()
    }

    fun save(key: String, snapshot: Map<String, String>) {
        val json = JSONObject()
        snapshot.forEach { (id, signature) -> json.put(id, signature) }
        prefs.edit().putString(key, json.toString()).apply()
    }
}
