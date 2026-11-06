package com.stupidtree.hita.ui.share

import com.stupidtree.hita.ui.share.protocol.ShareMode
import com.stupidtree.hita.ui.share.protocol.SharedOccurrence
import com.stupidtree.hita.ui.share.protocol.SharedPeriod
import com.stupidtree.hita.ui.share.protocol.SharedTerm
import com.stupidtree.hita.ui.share.protocol.SharedTimetable
import com.stupidtree.hita.ui.share.protocol.TimetableShareCodec
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.TermSubject
import com.stupidtree.hitax.data.model.timetable.TimeInDay
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.data.model.timetable.Timetable
import java.sql.Timestamp
import java.util.UUID

/** HITA1 v1 快照与宿主课表模型之间的转换。 */
internal object ShareMapper {

    data class Bundle(
        val timetable: Timetable,
        val subjects: List<TermSubject>,
        val events: List<EventItem>,
    )

    fun snapshot(timetable: Timetable, allEvents: List<EventItem>, nickname: String): SharedTimetable {
        val events = allEvents.filter {
            it.timetableId == timetable.id && it.type == EventItem.TYPE.CLASS
        }
        require(events.isNotEmpty()) { "课表没有可分享的课程" }
        val groups = events.groupBy { it.subjectId.ifBlank { it.name } }.values.toList()
        val courses = groups.map { it.first().name }
        val occurrences = groups.flatMapIndexed { index, group ->
            group.map {
                SharedOccurrence(
                    startMillis = it.from.time,
                    endMillis = it.to.time,
                    courseIndex = index,
                    place = it.place.orEmpty(),
                )
            }
        }
        val periods = timetable.scheduleStructure.map { period ->
            SharedPeriod(minutes(period.from), minutes(period.to))
        }
        val snapshot = SharedTimetable(
            shareId = UUID.nameUUIDFromBytes("hita-share:${timetable.id}".toByteArray()).toString(),
            nickname = nickname,
            mode = ShareMode.FULL,
            term = SharedTerm(
                timetableName = timetable.name ?: "课表",
                termName = timetable.name ?: "课表",
                startMillis = timetable.startTime.time,
                endMillis = timetable.endTime.time,
            ),
            periods = periods,
            courses = courses,
            occurrences = occurrences,
        )
        val codec = TimetableShareCodec()
        return codec.decodeJson(codec.canonicalJson(snapshot))
    }

    fun bundle(snapshot: SharedTimetable): Bundle {
        require(snapshot.mode == ShareMode.FULL) { "忙闲口令不包含课程详情" }
        TimetableShareCodec().validate(snapshot)
        val timetable = Timetable().apply {
            name = snapshot.term.timetableName.ifBlank { snapshot.term.termName }
            startTime = Timestamp(snapshot.term.startMillis)
            endTime = Timestamp(snapshot.term.endMillis)
            scheduleStructure = snapshot.periods.map {
                TimePeriodInDay(
                    TimeInDay(it.startMinute / 60, it.startMinute % 60),
                    TimeInDay(it.endMinute / 60, it.endMinute % 60),
                )
            }
        }
        val subjects = snapshot.courses.mapIndexed { index, name ->
            TermSubject().apply {
                timetableId = timetable.id
                this.name = name
                code = "SHARE_$index"
            }
        }
        val events = snapshot.occurrences.map { occurrence ->
            EventItem().apply {
                type = EventItem.TYPE.CLASS
                name = subjects[requireNotNull(occurrence.courseIndex)].name
                place = occurrence.place.orEmpty()
                subjectId = subjects[requireNotNull(occurrence.courseIndex)].id
                timetableId = timetable.id
                from = Timestamp(occurrence.startMillis)
                to = Timestamp(occurrence.endMillis)
            }
        }
        return Bundle(timetable, subjects, events)
    }

    private fun minutes(value: TimeInDay): Int = value.hour * 60 + value.minute
}
