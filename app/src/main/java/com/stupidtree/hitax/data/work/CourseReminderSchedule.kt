package com.stupidtree.hitax.data.work

import com.stupidtree.hitax.data.model.timetable.EventItem

/**
 * 课程提醒的时间窗口与去重键。
 *
 * 开始/结束提醒使用不同前缀，同一个 event 的两条通知互不吞掉。
 */
object CourseReminderSchedule {

    const val START_PREFIX = "start:"
    const val END_PREFIX = "end:"

    fun startDue(
        events: List<EventItem>,
        now: Long,
        leadMinutes: Int,
        sent: Set<String>,
    ): List<EventItem> {
        val windowEnd = now + leadMinutes * 60L * 1000
        return events.filter { event ->
            event.from.time in (now + 1) until windowEnd &&
                event.id !in sent &&
                START_PREFIX + event.id !in sent
        }
    }

    fun endDue(
        events: List<EventItem>,
        now: Long,
        leadMinutes: Int,
        windowEnd: Long,
        sent: Set<String>,
    ): List<EventItem> {
        val leadMs = leadMinutes * 60L * 1000
        return events.filter { event ->
            event.type == EventItem.TYPE.CLASS &&
                event.from.time < now &&
                (event.to.time - leadMs) in now until windowEnd &&
                END_PREFIX + event.id !in sent
        }
    }

    fun startKeys(events: List<EventItem>): List<String> =
        events.map { START_PREFIX + it.id }

    fun endKeys(events: List<EventItem>): List<String> =
        events.map { END_PREFIX + it.id }
}
