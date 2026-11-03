package com.stupidtree.hitax.data.repository

import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.TermSubject
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.component.util.ColorTools
import net.fortuna.ical4j.model.component.VEvent
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.LinkedHashMap
import java.util.Locale

data class IcsImportBundle(
    val timetable: Timetable,
    val subjects: List<TermSubject>,
    val events: List<EventItem>
)

data class IcsImportResult(
    val timetableId: String,
    val timetableName: String,
    val importedCount: Int
)

object IcsImportBundleBuilder {
    fun build(
        events: List<VEvent>,
        sourceName: String?,
        now: Long = System.currentTimeMillis(),
        nextColor: () -> Int = ColorTools::randomColorMaterial
    ): IcsImportBundle {
        val validEvents = events.mapNotNull { raw ->
            val courseName = raw.summary?.value?.trim().orEmpty().ifBlank { "未知课程" }
            val startDate = raw.startDate?.date ?: return@mapNotNull null
            val endDate = raw.endDate?.date ?: return@mapNotNull null
            ParsedIcsEvent(
                rawEvent = raw,
                courseName = courseName,
                startTime = startDate.time,
                endTime = endDate.time
            )
        }.sortedWith(compareBy<ParsedIcsEvent> { it.startTime }.thenBy { it.endTime })

        require(validEvents.isNotEmpty()) { "empty events" }

        val timetable = Timetable().apply {
            name = resolveTimetableName(sourceName, now)
            startTime = Timestamp(resolveWeekStart(validEvents.first().startTime))
            endTime = Timestamp(validEvents.maxOf { it.endTime })
        }

        val subjectsByName = LinkedHashMap<String, TermSubject>()
        val importedEvents = mutableListOf<EventItem>()
        for (event in validEvents) {
            val subject = subjectsByName.getOrPut(event.courseName) {
                TermSubject().apply {
                    name = event.courseName
                    timetableId = timetable.id
                    color = nextColor()
                }
            }
            val item = IcsImportEventMapper.map(
                event = event.rawEvent,
                timetableId = timetable.id,
                subjectId = subject.id,
                type = EventItem.TYPE.CLASS
            ) ?: continue
            importedEvents.add(item)
        }

        return IcsImportBundle(
            timetable = timetable,
            subjects = subjectsByName.values.toList(),
            events = importedEvents
        )
    }

    private fun resolveTimetableName(sourceName: String?, now: Long): String {
        val trimmed = sourceName?.trim().orEmpty()
        val withoutExtension = trimmed.replace(Regex("\\.ics$", RegexOption.IGNORE_CASE), "")
            .trim()
        if (withoutExtension.isNotEmpty()) {
            return withoutExtension
        }
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        return "ICS导入 ${formatter.format(now)}"
    }

    private fun resolveWeekStart(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private data class ParsedIcsEvent(
        val rawEvent: VEvent,
        val courseName: String,
        val startTime: Long,
        val endTime: Long
    )
}
