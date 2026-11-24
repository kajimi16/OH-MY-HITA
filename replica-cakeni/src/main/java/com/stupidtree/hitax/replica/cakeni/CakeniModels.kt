package com.stupidtree.hitax.replica.cakeni

import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.data.model.timetable.Timetable
import java.util.Calendar

private const val DAY_MS = 24L * 60 * 60 * 1000
private const val WEEK_MS = 7L * DAY_MS

/** 课程块只带 UI 需要的不可变字段；不把可变 EventItem 传进 Compose。 */
data class CakeniCourse(
    val id: String,
    val name: String,
    val teacher: String,
    val place: String,
    val dayIndex: Int,
    val from: Long,
    val to: Long,
    val startSlot: Int,
    val endSlot: Int,
    val color: Int,
    val type: EventItem.TYPE,
)

data class CakeniDay(
    val dayIndex: Int,
    val dateMillis: Long,
    val courses: List<CakeniCourse>,
)

data class CakeniWeek(
    val week: Int,
    val weekStart: Long,
    val timetableName: String,
    val days: List<CakeniDay>,
) {
    val courseCount: Int get() = days.sumOf { it.courses.size }
}

data class CakeniBlock(
    val course: CakeniCourse,
    val lane: Int,
    val laneCount: Int,
    val startSlot: Int,
    val span: Int,
)

fun weekStart(timetable: Timetable, week: Int): Long {
    val raw = timetable.startTime.time + (week - 1).coerceAtLeast(0) * WEEK_MS
    return mondayStart(raw)
}

fun weekCount(timetable: Timetable): Int {
    val span = timetable.endTime.time - timetable.startTime.time
    return ((span / WEEK_MS).toInt() + 1).coerceAtLeast(1)
}

fun currentWeek(timetable: Timetable, now: Long = System.currentTimeMillis()): Int {
    val week = timetable.getWeekNumber(now)
    return week.coerceIn(1, weekCount(timetable))
}

/**
 * 把服务层事件映射成一周的不可变模型。
 *
 * 后置条件：所有课程 dayIndex 在 0..6；startSlot/endSlot 在 1..节次数；
 * 重叠课程保留原顺序，由 [layoutCakeniDay] 分栏。
 */
fun buildCakeniWeek(
    events: List<EventItem>,
    timetable: Timetable,
    week: Int,
    dayCount: Int = 7,
): CakeniWeek {
    val start = weekStart(timetable, week)
    val end = start + WEEK_MS
    val structure = timetable.scheduleStructure
    val byDay = Array(7) { mutableListOf<CakeniCourse>() }
    events.asSequence()
        .filter { it.from.time in start until end }
        .map { toCourse(it, start, structure) }
        .filter { it.dayIndex in 0..6 }
        .distinctBy {
            listOf(it.name, it.teacher, it.place, it.dayIndex, it.startSlot, it.endSlot)
        }
        .forEach { byDay[it.dayIndex] += it }
    val safeDayCount = dayCount.coerceIn(5, 7)
    val days = (0 until safeDayCount).map { index ->
        CakeniDay(
            dayIndex = index,
            dateMillis = start + index * DAY_MS,
            courses = byDay[index].sortedWith(compareBy({ it.startSlot }, { it.name })),
        )
    }
    return CakeniWeek(
        week = week,
        weekStart = start,
        timetableName = timetable.name.orEmpty().ifBlank { "我的课表" },
        days = days,
    )
}

/**
 * 同一天重叠课程分栏；laneCount 按重叠簇计算，不能让一个课时的 4 门课
 * 把当天其他单门课也压成 1/4 宽。
 */
fun layoutCakeniDay(courses: List<CakeniCourse>): List<CakeniBlock> {
    val sorted = courses.sortedWith(compareBy({ it.startSlot }, { it.endSlot }))
    val clusters = mutableListOf<MutableList<CakeniCourse>>()
    for (course in sorted) {
        val last = clusters.lastOrNull()
        if (last != null && last.any { it.endSlot >= course.startSlot }) {
            last += course
        } else {
            clusters += mutableListOf(course)
        }
    }
    return clusters.flatMap { cluster ->
        val active = mutableListOf<MutableList<CakeniCourse>>()
        val laneOf = mutableMapOf<String, Int>()
        for (course in cluster) {
            active.removeAll { lane -> lane.none { it.endSlot >= course.startSlot } }
            val lane = active.indexOfFirst { it.none { c -> c.endSlot >= course.startSlot } }
                .takeIf { it >= 0 } ?: active.size.also { active.add(mutableListOf()) }
            active[lane] += course
            laneOf[course.id] = lane
        }
        val laneCount = (laneOf.values.maxOrNull() ?: 0) + 1
        cluster.map { course ->
            CakeniBlock(
                course = course,
                lane = laneOf[course.id] ?: 0,
                laneCount = laneCount,
                startSlot = course.startSlot,
                span = (course.endSlot - course.startSlot + 1).coerceAtLeast(1),
            )
        }
    }
}

private fun toCourse(
    event: EventItem,
    weekStart: Long,
    structure: List<TimePeriodInDay>,
): CakeniCourse {
    val slotCount = structure.size.coerceAtLeast(1)
    val dayIndex = ((event.from.time - weekStart) / DAY_MS).toInt()
    val fallbackStart = slotOf(event.from.time, structure)
    val startSlot = event.fromNumber.takeIf { it > 0 } ?: fallbackStart
    // HITA 的 lastNumber 是持续节数，不是结束节次：from=5,last=2 => 5..6。
    val endSlot = (startSlot + event.lastNumber.coerceAtLeast(1) - 1)
        .coerceAtLeast(startSlot)
    return CakeniCourse(
        id = event.id,
        name = event.name.ifBlank { "未命名课程" },
        teacher = event.teacher.orEmpty(),
        place = event.place.orEmpty(),
        dayIndex = dayIndex,
        from = event.from.time,
        to = event.to.time,
        startSlot = startSlot.coerceIn(1, slotCount.coerceAtLeast(1)),
        endSlot = endSlot.coerceIn(startSlot, slotCount.coerceAtLeast(startSlot)),
        color = event.color.takeIf { it != 0 } ?: fallbackColor(event.name),
        type = event.type,
    )
}

private fun slotOf(ts: Long, structure: List<TimePeriodInDay>): Int {
    if (structure.isEmpty()) return 1
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    val minutes = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    val index = structure.indexOfFirst {
        minutes < it.to.hour * 60 + it.to.minute
    }.takeIf { it >= 0 } ?: structure.lastIndex
    return index + 1
}

private fun fallbackColor(name: String): Int {
    val palette = intArrayOf(
        0xFF7C9A92.toInt(),
        0xFF8A7FA8.toInt(),
        0xFFB08968.toInt(),
        0xFF6C8EBF.toInt(),
        0xFFA76D6D.toInt(),
        0xFF7E9B5A.toInt(),
    )
    return palette[(name.hashCode() and Int.MAX_VALUE) % palette.size]
}

fun mondayStart(ts: Long): Long {
    val c = Calendar.getInstance().apply {
        timeInMillis = ts
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return c.timeInMillis
}
