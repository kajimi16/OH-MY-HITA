package com.stupidtree.hitax.plugin

import com.stupidtree.hita.api.dto.EventDto
import com.stupidtree.hita.api.dto.SubjectColorDto
import com.stupidtree.hita.api.dto.SubjectDto
import com.stupidtree.hita.api.dto.TeacherDto
import com.stupidtree.hita.api.dto.TimePeriodDto
import com.stupidtree.hita.api.dto.TimetableDefaults
import com.stupidtree.hita.api.dto.TimetableDto
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.SubjectColor
import com.stupidtree.hitax.data.model.timetable.TeacherInfo
import com.stupidtree.hitax.data.model.timetable.TermSubject
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.data.model.timetable.Timetable
import java.sql.Timestamp

/**
 * 实体 ↔ DTO 映射。只允许插件内部使用；实体永远不越过 `TimetableDataService`。
 *
 * 所有时间都用 `Timestamp.time` 的 epoch millis；绝不使用 `Timestamp.toString()` 或时区解析。
 * `@Ignore` 字段（teacher / externalCourseId / taskNumber / selectionRequestId）必须保留，
 * 否则教务导入或选课链路会静默丢字段。
 */

internal fun TimePeriodInDay.toDto(): TimePeriodDto = TimePeriodDto(
    fromHour = from.hour,
    fromMinute = from.minute,
    toHour = to.hour,
    toMinute = to.minute,
)

internal fun TimePeriodDto.toEntity(): TimePeriodInDay = TimePeriodInDay(
    com.stupidtree.hitax.data.model.timetable.TimeInDay(fromHour, fromMinute),
    com.stupidtree.hitax.data.model.timetable.TimeInDay(toHour, toMinute),
)

internal fun Timetable.toDto(): TimetableDto = TimetableDto(
    id = id,
    name = name,
    code = code,
    startTime = startTime.time,
    endTime = endTime.time,
    createdAt = createdAt.time,
    periods = scheduleStructure.map { it.toDto() },
)

internal fun TimetableDto.toEntity(): Timetable = Timetable().apply {
    id = this@toEntity.id
    name = this@toEntity.name
    code = this@toEntity.code
    startTime = Timestamp(this@toEntity.startTime)
    endTime = Timestamp(this@toEntity.endTime)
    createdAt = Timestamp(this@toEntity.createdAt)
    scheduleStructure = if (this@toEntity.periods.isEmpty()) {
        TimetableDefaults.periods().map { it.toEntity() }
    } else {
        this@toEntity.periods.map { it.toEntity() }
    }
}

internal fun EventItem.toDto(): EventDto = EventDto(
    id = id,
    type = type.name,
    name = name,
    place = place,
    teacher = teacher,
    subjectId = subjectId,
    timetableId = timetableId,
    from = from.time,
    to = to.time,
    fromNumber = fromNumber,
    lastNumber = lastNumber,
    createdAt = createdAt.time,
    color = color,
)

internal fun EventDto.toEntity(): EventItem = EventItem().apply {
    id = this@toEntity.id
    type = eventTypeOf(this@toEntity.type)
    name = this@toEntity.name
    place = this@toEntity.place
    teacher = this@toEntity.teacher
    subjectId = this@toEntity.subjectId
    timetableId = this@toEntity.timetableId
    from = Timestamp(this@toEntity.from)
    to = Timestamp(this@toEntity.to)
    fromNumber = this@toEntity.fromNumber
    lastNumber = this@toEntity.lastNumber
    createdAt = Timestamp(this@toEntity.createdAt)
    color = this@toEntity.color
}

internal fun TermSubject.toDto(): SubjectDto = SubjectDto(
    id = id,
    name = name,
    timetableId = timetableId,
    type = type.name,
    field = field,
    selectCategory = selectCategory,
    nature = nature,
    credit = credit,
    school = school,
    countInSpa = countInSPA,
    code = code,
    key = key,
    teacher = teacher,
    externalCourseId = externalCourseId,
    taskNumber = taskNumber,
    selectionRequestId = selectionRequestId,
    createdAt = createdAt.time,
    color = color,
)

internal fun SubjectDto.toEntity(): TermSubject = TermSubject().apply {
    id = this@toEntity.id
    name = this@toEntity.name
    timetableId = this@toEntity.timetableId
    type = subjectTypeOf(this@toEntity.type)
    field = this@toEntity.field
    selectCategory = this@toEntity.selectCategory
    nature = this@toEntity.nature
    credit = this@toEntity.credit
    school = this@toEntity.school
    countInSPA = this@toEntity.countInSpa
    code = this@toEntity.code
    key = this@toEntity.key
    teacher = this@toEntity.teacher
    externalCourseId = this@toEntity.externalCourseId
    taskNumber = this@toEntity.taskNumber
    selectionRequestId = this@toEntity.selectionRequestId
    createdAt = Timestamp(this@toEntity.createdAt)
    color = this@toEntity.color
}

internal fun SubjectColor.toDto(): SubjectColorDto = SubjectColorDto(id = id, color = color)

internal fun SubjectColorDto.toEntity(): SubjectColor = SubjectColor().apply {
    id = this@toEntity.id
    color = this@toEntity.color
}

internal fun TeacherInfo.toDto(): TeacherDto = TeacherDto(name = name, subjectName = subjectName)

internal fun TeacherDto.toEntity(): TeacherInfo = TeacherInfo().apply {
    name = this@toEntity.name
    subjectName = this@toEntity.subjectName
}

private fun eventTypeOf(value: String): EventItem.TYPE =
    EventItem.TYPE.values().firstOrNull { it.name == value } ?: EventItem.TYPE.OTHER

private fun subjectTypeOf(value: String): TermSubject.TYPE =
    TermSubject.TYPE.values().firstOrNull { it.name == value } ?: TermSubject.TYPE.COM_A
