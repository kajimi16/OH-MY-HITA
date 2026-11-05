package com.stupidtree.hitax.data.repository

import com.stupidtree.hitax.data.model.timetable.EventItem

/**
 * 主课表显示层的重复课程合并。
 *
 * 历史版本可能留下内容相同的重复课表（例如重复导入的默认课表），DAO 按时间窗口
 * 查询时会把同一门课取回多次，课表就会把它当成时间冲突并排画成两张卡。这里按
 * 课程内容去重，保留第一条；只处理 CLASS，不合并手动事件、考试或真正不同的课程。
 */
internal object TimetableEventDedupe {
    fun apply(events: List<EventItem>): List<EventItem> {
        val seen = mutableSetOf<String>()
        return events.filter { event ->
            event.type != EventItem.TYPE.CLASS || seen.add(identityKey(event))
        }
    }

    internal fun identityKey(event: EventItem): String = listOf(
        event.name.trim(),
        event.place.orEmpty().trim(),
        event.teacher.orEmpty().trim(),
        event.from.time.toString(),
        event.to.time.toString(),
        event.fromNumber.toString(),
        event.lastNumber.toString(),
    ).joinToString("|")
}
