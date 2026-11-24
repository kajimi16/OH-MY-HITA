package com.stupidtree.hitax.shell

import com.stupidtree.hitax.data.model.timetable.EventItem

enum class TimetableChangeKind { ADDED, REMOVED, MOVED, DETAILS }

data class TimetableChange(
    val kind: TimetableChangeKind,
    val before: EventItem?,
    val after: EventItem?,
)

data class TimetableDiff(
    val changes: List<TimetableChange>,
) {
    fun count(kind: TimetableChangeKind): Int = changes.count { it.kind == kind }
    fun isEmpty(): Boolean = changes.isEmpty()
}

/**
 * 课表版本比较的纯函数。
 *
 * 以事件 id 为身份；id 缺失、新增、删除、时间/地点/教师变化分别归类。
 */
object TimetableHistoryDiff {

    fun fingerprint(events: List<EventItem>): String =
        events.filter { it.type == EventItem.TYPE.CLASS }
            .sortedWith(compareBy({ it.id }, { it.from.time }, { it.name }))
            .joinToString("|") {
                listOf(
                    it.id,
                    it.subjectId,
                    it.name,
                    it.place.orEmpty(),
                    it.teacher.orEmpty(),
                    it.from.time.toString(),
                    it.to.time.toString(),
                ).joinToString("~")
            }

    fun eventsFor(timetableId: String, events: List<EventItem>): List<EventItem> =
        events.filter { it.type == EventItem.TYPE.CLASS && it.timetableId == timetableId }

    fun diff(before: List<EventItem>, after: List<EventItem>): TimetableDiff {
        val previous = before.filter { it.type == EventItem.TYPE.CLASS }.associateBy { it.id }
        val current = after.filter { it.type == EventItem.TYPE.CLASS }.associateBy { it.id }
        val changes = buildList {
            current.forEach { (id, event) ->
                val old = previous[id]
                if (old == null) {
                    add(TimetableChange(TimetableChangeKind.ADDED, null, event))
                } else {
                    when {
                        old.from.time != event.from.time || old.to.time != event.to.time ->
                            add(TimetableChange(TimetableChangeKind.MOVED, old, event))
                        old.place != event.place || old.teacher != event.teacher ||
                            old.name != event.name ->
                            add(TimetableChange(TimetableChangeKind.DETAILS, old, event))
                    }
                }
            }
            previous.forEach { (id, event) ->
                if (id !in current) add(TimetableChange(TimetableChangeKind.REMOVED, event, null))
            }
        }
        return TimetableDiff(changes)
    }
}
