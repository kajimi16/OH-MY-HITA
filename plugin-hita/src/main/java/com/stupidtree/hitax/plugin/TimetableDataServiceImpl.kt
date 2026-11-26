package com.stupidtree.hitax.plugin

import android.app.Application
import androidx.annotation.WorkerThread
import androidx.room.InvalidationTracker
import com.stupidtree.component.util.ColorTools
import com.stupidtree.hita.api.TimetableDataService
import com.stupidtree.hita.api.dto.EventDto
import com.stupidtree.hita.api.dto.IcsExportResultDto
import com.stupidtree.hita.api.dto.IcsImportRequestDto
import com.stupidtree.hita.api.dto.IcsImportResultDto
import com.stupidtree.hita.api.dto.SubjectColorDto
import com.stupidtree.hita.api.dto.SubjectDto
import com.stupidtree.hita.api.dto.SubjectProgressDto
import com.stupidtree.hita.api.dto.TeacherDto
import com.stupidtree.hita.api.dto.TimePeriodDto
import com.stupidtree.hita.api.dto.TimetableChangeDto
import com.stupidtree.hita.api.dto.TimetableDto
import com.stupidtree.hita.api.dto.TimetableSnapshotDto
import com.stupidtree.hitax.data.AppDatabase
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.TermSubject
import com.stupidtree.hitax.data.model.timetable.TimeMath
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.data.model.timetable.TeacherInfo
import com.stupidtree.hitax.data.model.timetable.WEEK_MILLS
import com.stupidtree.hitax.data.repository.IcsImportBundleBuilder
import com.stupidtree.hitax.data.repository.IcsImportEventMapper
import com.stupidtree.hitax.data.repository.TimetableEventDedupe
import com.stupidtree.hitax.data.repository.TimetableRepository
import com.stupidtree.style.CutePaletteStore
import com.stupidtree.sync.StupidSync
import com.stupidtree.sync.data.model.History
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.Dur
import net.fortuna.ical4j.model.component.VAlarm
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.Action
import net.fortuna.ical4j.model.property.CalScale
import net.fortuna.ical4j.model.property.Description
import net.fortuna.ical4j.model.property.Location
import net.fortuna.ical4j.model.property.ProdId
import net.fortuna.ical4j.model.property.Summary
import net.fortuna.ical4j.model.property.Uid
import net.fortuna.ical4j.model.property.Version
import net.fortuna.ical4j.util.UidGenerator
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.sql.Timestamp
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 课表 DTO 实现。读/写直接走 Room 同步 DAO，不把 LiveData 包成 runBlocking。
 * 旧 [com.stupidtree.hita.api.TimetableService] 保留不动。
 */
class TimetableDataServiceImpl(
    private val application: Application,
    private val timetables: TimetableRepository,
    private val database: AppDatabase,
) : TimetableDataService {

    private val timetableDao = database.timetableDao()
    private val eventDao = database.eventItemDao()
    private val subjectDao = database.subjectDao()

    @WorkerThread
    override fun snapshot(): TimetableSnapshotDto =
        TimetableSnapshotDto(timetables = timetables())

    @WorkerThread
    override fun timetables(): List<TimetableDto> =
        timetableDao.getTimetablesSync().map { it.toDto() }

    @WorkerThread
    override fun timetableById(id: String): TimetableDto? =
        timetableDao.getTimetableByIdSync(id)?.toDto()

    @WorkerThread
    override fun timetableByEasCode(code: String): TimetableDto? =
        timetableDao.getTimetableByEASCodeSync(code)?.toDto()

    @WorkerThread
    override fun timetableByEasCode(code: String, providerId: String): TimetableDto? =
        timetables.getTimetableByEasCodeSync(code, providerId)?.toDto()

    @WorkerThread
    override fun currentWeek(easCode: String?): Int =
        timetableDao.getTimetableByEASCodeSync(easCode ?: "")
            ?.getWeekNumber(System.currentTimeMillis()) ?: 1

    @WorkerThread
    override fun currentWeek(easCode: String?, providerId: String): Int {
        require(providerId.isNotBlank()) { "providerId must not be blank" }
        val code = easCode?.takeIf { it.isNotBlank() } ?: return 1
        return timetables.getTimetableByEasCodeSync(code, providerId)
            ?.getWeekNumber(System.currentTimeMillis()) ?: 1
    }

    @WorkerThread
    override fun recentTimetable(): TimetableDto? =
        timetableDao.getTimetableClosestToTimestampSync(System.currentTimeMillis())?.toDto()

    @WorkerThread
    override fun timetableCount(): Int = timetableDao.getTimetableCountSync()

    @WorkerThread
    override fun eventsDuring(from: Long, to: Long): List<EventDto> {
        requireValidEventRange(from, to)
        return TimetableEventDedupe.apply(timetables.getEventsDuringSync(from, to))
            .map { it.toDto() }
    }

    @WorkerThread
    override fun eventsAfter(from: Long, limit: Int): List<EventDto> {
        requireValidLimit(limit)
        return eventDao.getEventsAfterSync(from, limit).map { it.toDto() }
    }

    @WorkerThread
    override fun eventsDuringWithColor(from: Long, to: Long): List<EventDto> {
        requireValidEventRange(from, to)
        val events = TimetableEventDedupe.apply(timetables.getEventsDuringSync(from, to))
        val subjectIds = events.map { it.subjectId }.filter { it.isNotBlank() }.toSet()
        val colors = if (subjectIds.isEmpty()) {
            emptyMap()
        } else {
            subjectDao.getSubjectsInIdsSync(subjectIds.toList()).associate { it.id to it.color }
        }
        events.forEach { it.color = colors[it.subjectId] ?: 0 }
        return events.map { it.toDto() }
    }

    @WorkerThread
    override fun todayEvents(): List<EventDto> =
        timetables.getTodayEventsSync().map { it.toDto() }

    @WorkerThread
    override fun classesOfSubject(subjectId: String): List<EventDto> =
        eventDao.getClassesOfSubjectSync(subjectId).map { it.toDto() }

    @WorkerThread
    override fun searchLocations(query: String): List<String> =
        timetableDao.searchLocationSync("%$query%")

    @WorkerThread
    override fun subjects(timetableId: String): List<SubjectDto> =
        subjectDao.getSubjectsSync(timetableId)
            .sortedByDescending { it.credit }
            .map { it.toDto() }

    @WorkerThread
    override fun subjectById(subjectId: String): SubjectDto? =
        subjectDao.getSubjectsInIdsSync(listOf(subjectId)).firstOrNull()?.toDto()

    @WorkerThread
    override fun subjectColor(subjectId: String): SubjectColorDto? =
        subjectDao.getSubjectsInIdsSync(listOf(subjectId)).firstOrNull()
            ?.let { SubjectColorDto(id = it.id, color = it.color) }

    @WorkerThread
    override fun teachersInfo(timetableId: String): List<TeacherDto> {
        val map = mutableMapOf<String, TeacherInfo>()
        for (teacher in eventDao.getTeachersOfTimetableSync(timetableId)) {
            val names = splitTeachers(teacher.name)
            if (names.isEmpty()) continue
            for (name in names) {
                val existed = map[name]
                if (existed == null) {
                    map[name] = TeacherInfo().apply {
                        this.name = name
                        this.subjectName = teacher.subjectName
                    }
                } else {
                    existed.subjectName = listOf(existed.subjectName, teacher.subjectName)
                        .filter { !it.isNullOrBlank() }
                        .joinToString(" ")
                }
            }
        }
        return map.values.map { it.toDto() }
    }

    @WorkerThread
    override fun teachersOfSubject(timetableId: String, subjectId: String): List<String> {
        val result = linkedSetOf<String>()
        for (raw in eventDao.getTeachersOfSubjectSync(timetableId, subjectId)) {
            splitTeachers(raw).forEach { if (it.isNotBlank()) result.add(it) }
        }
        return result.toList()
    }

    @WorkerThread
    override fun progressOfSubject(subjectId: String, ts: Long): SubjectProgressDto =
        SubjectProgressDto(
            completed = eventDao.countClassesBeforeTimeOfSubjectSync(subjectId, ts),
            total = eventDao.countClassesOfSubjectSync(subjectId),
        )

    override fun observeChanges(listener: (TimetableChangeDto) -> Unit): AutoCloseable {
        var observer: InvalidationTracker.Observer? = null
        val subscription = ChangeSubscription {
            // observer 在下面初始化；Room 的 addObserver 不会在注册时同步回调。
            observer?.let { database.invalidationTracker.removeObserver(it) }
        }
        observer = object : InvalidationTracker.Observer("timetable", "subject", "events") {
            override fun onInvalidated(tables: Set<String>) {
                subscription.emit {
                    listener(TimetableChangeDto(kind = changeKind(tables)))
                }
            }
        }
        database.invalidationTracker.addObserver(observer!!)
        return subscription
    }

    @WorkerThread
    override fun eventIdsOfTimetables(ids: List<String>): List<String> =
        eventDao.getEventIdsFromTimetablesSync(ids)

    @WorkerThread
    override fun subjectByName(timetableId: String, name: String?): SubjectDto? =
        subjectDao.getSubjectByName(timetableId, name)?.toDto()

    @WorkerThread
    override fun createTimetable(): TimetableDto {
        val defaultTables = timetableDao.getTimetableNamesWithDefaultSync(DEFAULT_TIMETABLE_NAME + "%")
        var max = 0
        for (tt in defaultTables) {
            val i = tt.replace(DEFAULT_TIMETABLE_NAME, "").toIntOrNull()
            if (i != null && i > max) max = i
        }
        val newTable = Timetable().apply {
            val monday = TimeMath.getMonday(System.currentTimeMillis())
            startTime.time = monday.timeInMillis
            endTime.time = monday.timeInMillis + WEEK_MILLS
            name = DEFAULT_TIMETABLE_NAME + (max + 1)
        }
        timetableDao.saveTimetableSync(newTable)
        StupidSync.putHistorySync("timetable", History.ACTION.REQUIRE, listOf(newTable.id))
        return newTable.toDto()
    }

    @WorkerThread
    override fun upsertTimetable(timetable: TimetableDto) {
        requireValidId(timetable.id, "timetable")
        val providerId = timetable.providerId
        require(providerId == null || providerId.isNotBlank()) {
            "providerId must not be blank"
        }
        timetableDao.saveTimetableSync(timetable.toEntity())
        StupidSync.putHistorySync("timetable", History.ACTION.REQUIRE, listOf(timetable.id))
    }

    @WorkerThread
    override fun upsertSubject(subject: SubjectDto) {
        requireValidId(subject.id, "subject")
        subjectDao.saveSubjectSync(subject.toEntity())
        StupidSync.putHistorySync("subject", History.ACTION.REQUIRE, listOf(subject.id))
    }

    @WorkerThread
    override fun upsertEvents(events: List<EventDto>) {
        if (events.isEmpty()) return
        val entities = events.map {
            requireValidId(it.id, "event")
            it.toEntity()
        }
        eventDao.saveEvents(entities)
        StupidSync.putHistorySync("event", History.ACTION.REQUIRE, entities.map { it.id })
    }

    @WorkerThread
    override fun deleteTimetables(ids: List<String>) {
        if (ids.isEmpty()) return
        val eventIds = eventDao.getEventIdsFromTimetablesSync(ids)
        val subjectIds = subjectDao.getSubjectIdsOfTimetablesSync(ids)
        StupidSync.putHistorySync("event", History.ACTION.REMOVE, eventIds)
        StupidSync.putHistorySync("subject", History.ACTION.REMOVE, subjectIds)
        timetableDao.deleteTimetablesInIdsSync(ids)
        eventDao.deleteEventsFromTimetablesSync(ids)
        subjectDao.deleteSubjectsFromTimetablesSync(ids)
        StupidSync.putHistorySync("timetable", History.ACTION.REMOVE, ids)
    }

    @WorkerThread
    override fun deleteEvents(ids: List<String>) {
        if (ids.isEmpty()) return
        StupidSync.putHistorySync("event", History.ACTION.REMOVE, ids)
        eventDao.deleteEventsInIdsSync(ids)
    }

    @WorkerThread
    override fun deleteCoursesFromTimetable(timetableId: String) =
        eventDao.deleteCourseFromTimetable(timetableId)

    @WorkerThread
    override fun deleteSubjects(ids: List<String>) {
        if (ids.isEmpty()) return
        subjectDao.deleteSubjectsInIdsSync(ids)
        eventDao.deleteEventsFromSubjectsSync(ids)
        StupidSync.putHistorySync("subject", History.ACTION.REMOVE, ids)
    }

    @WorkerThread
    override fun clearAll() {
        eventDao.clear()
        subjectDao.clear()
        timetableDao.clear()
    }

    @WorkerThread
    override fun changeStartDate(timetableId: String, startTime: Long) {
        val timetable = timetableDao.getTimetableByIdSync(timetableId) ?: return
        val monday = TimeMath.getMonday(startTime)
        val offset = monday.timeInMillis - timetable.startTime.time
        timetable.endTime.time += offset
        timetable.startTime.time = monday.timeInMillis
        timetableDao.saveTimetableSync(timetable)
        eventDao.updateClassesAddOffset(timetable.id, offset)
        StupidSync.putHistorySync("timetable", History.ACTION.REQUIRE, listOf(timetable.id))
    }

    @WorkerThread
    override fun changeStructure(timetableId: String, period: TimePeriodDto, position: Int) {
        requireValidPosition(position)
        val timetable = timetableDao.getTimetableByIdSync(timetableId) ?: return
        timetable.setScheduleStructure(period.toEntity(), position)
        timetableDao.saveTimetableSync(timetable)
        StupidSync.putHistorySync("timetable", History.ACTION.REQUIRE, listOf(timetable.id))

        val calendar = Calendar.getInstance()
        val changedIds = mutableListOf<String>()
        val fromToChange = eventDao.getClassAtFromNumberSync(timetable.id, position + 1)
        for (event in fromToChange) {
            changedIds += event.id
            calendar.timeInMillis = event.from.time
            calendar.set(Calendar.HOUR_OF_DAY, period.fromHour)
            calendar.set(Calendar.MINUTE, period.fromMinute)
            event.from.time = calendar.timeInMillis
        }
        eventDao.saveEvents(fromToChange)

        val endToChange = eventDao.getClassAtToNumberSync(timetable.id, position + 1)
        for (event in endToChange) {
            changedIds += event.id
            calendar.timeInMillis = event.to.time
            calendar.set(Calendar.HOUR_OF_DAY, period.toHour)
            calendar.set(Calendar.MINUTE, period.toMinute)
            event.to.time = calendar.timeInMillis
        }
        eventDao.saveEvents(endToChange)
        if (changedIds.isNotEmpty()) {
            StupidSync.putHistorySync("event", History.ACTION.REQUIRE, changedIds)
        }
    }

    @WorkerThread
    override fun changeSubjectColor(subjectId: String, color: Int) =
        subjectDao.changeSubjectColorSync(subjectId, color)

    @WorkerThread
    override fun resetRecentSubjectColors() {
        ColorTools.applyPalette(CutePaletteStore.colorsForActivePreset(application))
        val timetable = timetableDao.getTimetableClosestToTimestampSync(System.currentTimeMillis()) ?: return
        resetColors(timetable.id)
    }

    @WorkerThread
    override fun resetSubjectColors(timetableId: String) = resetColors(timetableId)

    private fun resetColors(timetableId: String) {
        val subjects = subjectDao.getSubjectsSync(timetableId)
        for (subject in subjects) subject.color = ColorTools.randomColorMaterial()
        subjectDao.saveSubjectsSync(subjects)
        StupidSync.putHistorySync("subject", History.ACTION.REQUIRE, subjects.map { it.id })
    }

    @WorkerThread
    override fun exportIcs(timetableName: String, timetableId: String): IcsExportResultDto {
        val filename = "HITSZ助手：$timetableName.ics"
        val dir = application.getExternalFilesDir("ics") ?: application.filesDir
        val file = File(dir, filename)
        val events = eventDao.getEventsOfTimetableSync(timetableId)
        val calendar = net.fortuna.ical4j.model.Calendar()
        calendar.properties.add(ProdId("-//StupidTree//HITA//EN"))
        calendar.properties.add(Version.VERSION_2_0)
        calendar.properties.add(CalScale.GREGORIAN)
        for (event in events) {
            val start = DateTime(event.from).apply { isUtc = true }
            val end = DateTime(event.to).apply { isUtc = true }
            val vevent = VEvent(start, end, event.name)
            vevent.properties.add(
                Location(event.place + " " + if (event.teacher.isNullOrEmpty()) "" else event.teacher)
            )
            vevent.properties.add(Uid(UidGenerator("hita").generateUid().value))
            val alarm = VAlarm(Dur(0, 0, -10, 0))
            alarm.properties.add(Summary("课程提醒"))
            alarm.properties.add(Action.DISPLAY)
            alarm.properties.add(Description(event.name + "马上就要开始啦！"))
            vevent.alarms.add(alarm)
            calendar.components.add(vevent)
        }
        calendar.validate()
        FileOutputStream(file).use { CalendarOutputter().output(calendar, it) }
        return IcsExportResultDto(filePath = file.absolutePath, fileName = filename, eventCount = events.size)
    }

    @WorkerThread
    override fun importIcs(request: IcsImportRequestDto, timetableId: String): IcsImportResultDto {
        val calendar = ByteArrayInputStream(request.bytes).use { CalendarBuilder().build(it) }
        val events = calendar.components.filterIsInstance<VEvent>()
        var importedCount = 0
        for (event in events) {
            val item = IcsImportEventMapper.map(event, timetableId) ?: continue
            eventDao.insertEventSync(item)
            importedCount++
        }
        if (importedCount > 0) {
            StupidSync.putHistorySync("event", History.ACTION.REQUIRE, eventDao.getEventIdsOfTimetableSync(timetableId))
        }
        return IcsImportResultDto(
            timetableId = timetableId,
            timetableName = timetableDao.getTimetableByIdSync(timetableId)?.name ?: "",
            importedCount = importedCount,
        )
    }

    @WorkerThread
    override fun importIcsAsNewTimetable(
        request: IcsImportRequestDto,
        sourceName: String?,
    ): IcsImportResultDto {
        val calendar = ByteArrayInputStream(request.bytes).use { CalendarBuilder().build(it) }
        val bundle = IcsImportBundleBuilder.build(
            events = calendar.components.filterIsInstance<VEvent>(),
            sourceName = sourceName,
        )
        timetableDao.saveTimetableSync(bundle.timetable)
        subjectDao.saveSubjectsSync(bundle.subjects)
        eventDao.saveEvents(bundle.events)
        StupidSync.putHistorySync("timetable", History.ACTION.REQUIRE, listOf(bundle.timetable.id))
        StupidSync.putHistorySync("subject", History.ACTION.REQUIRE, bundle.subjects.map { it.id })
        StupidSync.putHistorySync("event", History.ACTION.REQUIRE, bundle.events.map { it.id })
        return IcsImportResultDto(
            timetableId = bundle.timetable.id,
            timetableName = bundle.timetable.name ?: DEFAULT_TIMETABLE_NAME,
            importedCount = bundle.events.size,
        )
    }

    private fun splitTeachers(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(Regex("[,，、]")).map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun changeKind(tables: Set<String>): String = when {
        tables.size != 1 -> "all"
        tables.contains("events") -> "event"
        tables.contains("subject") -> "subject"
        tables.contains("timetable") -> "timetable"
        else -> "all"
    }

    private companion object {
        private const val DEFAULT_TIMETABLE_NAME = "新建课表"
    }
}

/**
 * 观察订阅的关闭闸门。`close()` 后 `emit` 不再执行；便于纯 JVM 测试。
 */
internal class ChangeSubscription(
    private val onClose: () -> Unit,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)

    fun emit(block: () -> Unit) {
        if (!closed.get()) block()
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) onClose()
    }
}


internal fun requireValidEventRange(from: Long, to: Long) {
    require(from <= to) { "from must be <= to" }
}

internal fun requireValidLimit(limit: Int) {
    require(limit >= 0) { "limit must be >= 0" }
}

internal fun requireValidPosition(position: Int) {
    require(position >= 0) { "position must be >= 0" }
}


internal fun requireValidId(id: String, label: String) {
    require(id.isNotBlank()) { "$label id must not be blank" }
}
