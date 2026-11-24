package com.stupidtree.hitax.calendar

import com.stupidtree.hitax.data.model.timetable.EventItem

data class CalendarSyncEntry(
    val key: String,
    val title: String,
    val location: String,
    val description: String,
    val start: Long,
    val end: Long,
)

data class CalendarSyncPlan(
    val inserts: List<CalendarSyncEntry>,
    val updates: List<CalendarSyncEntry>,
    val deletes: List<String>,
)

/**
 * 课表到系统日历的纯规划层。
 *
 * 只同步 CLASS 事件；`SYNC_DATA` 用 `hita:<timetableId>:<eventId>`，
 * 用于更新同一事件并清理已删除课程。
 */
object CalendarSyncPlanner {

    fun key(timetableId: String, eventId: String): String = "hita:$timetableId:$eventId"

    fun keyFromDescription(description: String?): String? =
        description?.lineSequence()?.firstOrNull { it.startsWith("hita:") }

    fun entries(
        events: List<EventItem>,
        timetableId: String,
        timetableName: String?,
    ): List<CalendarSyncEntry> = events
        .filter { it.type == EventItem.TYPE.CLASS && it.timetableId == timetableId }
        .filter { it.to.time > it.from.time }
        .sortedWith(compareBy({ it.from.time }, { it.name }))
        .map { event ->
            CalendarSyncEntry(
                key = key(timetableId, event.id),
                title = event.name,
                location = event.place.orEmpty(),
                description = buildString {
                    append(timetableName?.takeIf { it.isNotBlank() } ?: "HITA 课表")
                    event.teacher?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
                },
                start = event.from.time,
                end = event.to.time,
            )
        }

    fun plan(
        entries: List<CalendarSyncEntry>,
        existingKeys: Set<String>,
    ): CalendarSyncPlan {
        val incoming = entries.associateBy { it.key }
        return CalendarSyncPlan(
            inserts = entries.filter { it.key !in existingKeys },
            updates = entries.filter { it.key in existingKeys },
            deletes = existingKeys.filter { it !in incoming },
        )
    }
}
