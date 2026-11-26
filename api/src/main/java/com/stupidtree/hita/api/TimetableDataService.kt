package com.stupidtree.hita.api

import androidx.annotation.WorkerThread
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

/**
 * 课表纯数据契约（API 14）。
 *
 * 只返回 DTO；不出现 Room 实体、LiveData、Timestamp、Calendar 或 InputStream。
 * 所有读写方法都是阻塞的，调用方必须在工作线程执行。旧 [TimetableService] 原样保留。
 */
interface TimetableDataService {

    // ---- 读 ----

    @WorkerThread
    fun snapshot(): TimetableSnapshotDto

    @WorkerThread
    fun timetables(): List<TimetableDto>

    @WorkerThread
    fun timetableById(id: String): TimetableDto?

    @WorkerThread
    fun timetableByEasCode(code: String): TimetableDto?

    @WorkerThread
    fun currentWeek(easCode: String?): Int

    @WorkerThread
    fun recentTimetable(): TimetableDto?

    @WorkerThread
    fun timetableCount(): Int

    @WorkerThread
    fun eventsDuring(from: Long, to: Long): List<EventDto>

    @WorkerThread
    fun eventsAfter(from: Long, limit: Int): List<EventDto>

    @WorkerThread
    fun eventsDuringWithColor(from: Long, to: Long): List<EventDto>

    @WorkerThread
    fun todayEvents(): List<EventDto>

    @WorkerThread
    fun classesOfSubject(subjectId: String): List<EventDto>

    @WorkerThread
    fun searchLocations(query: String): List<String>

    @WorkerThread
    fun subjects(timetableId: String): List<SubjectDto>

    @WorkerThread
    fun subjectById(subjectId: String): SubjectDto?

    @WorkerThread
    fun subjectColor(subjectId: String): SubjectColorDto?

    @WorkerThread
    fun teachersInfo(timetableId: String): List<TeacherDto>

    @WorkerThread
    fun teachersOfSubject(timetableId: String, subjectId: String): List<String>

    @WorkerThread
    fun progressOfSubject(subjectId: String, ts: Long): SubjectProgressDto

    /**
     * 监听 timetable / subject / events 表变更。
     *
     * 回调线程由 Room 决定；`close()` 后不再回调。调用方必须关闭。
     */
    fun observeChanges(listener: (TimetableChangeDto) -> Unit): AutoCloseable

    // ---- 同步读写（教务导入使用） ----

    @WorkerThread
    fun eventIdsOfTimetables(ids: List<String>): List<String>

    @WorkerThread
    fun subjectByName(timetableId: String, name: String?): SubjectDto?

    // ---- 写（阻塞到落库） ----

    @WorkerThread
    fun createTimetable(): TimetableDto

    @WorkerThread
    fun upsertTimetable(timetable: TimetableDto)

    @WorkerThread
    fun upsertSubject(subject: SubjectDto)

    @WorkerThread
    fun upsertEvents(events: List<EventDto>)

    @WorkerThread
    fun deleteTimetables(ids: List<String>)

    @WorkerThread
    fun deleteEvents(ids: List<String>)

    @WorkerThread
    fun deleteCoursesFromTimetable(timetableId: String)

    @WorkerThread
    fun deleteSubjects(ids: List<String>)

    @WorkerThread
    fun clearAll()

    @WorkerThread
    fun changeStartDate(timetableId: String, startTime: Long)

    @WorkerThread
    fun changeStructure(timetableId: String, period: TimePeriodDto, position: Int)

    @WorkerThread
    fun changeSubjectColor(subjectId: String, color: Int)

    @WorkerThread
    fun resetRecentSubjectColors()

    @WorkerThread
    fun resetSubjectColors(timetableId: String)

    // ---- ICS ----

    @WorkerThread
    fun exportIcs(timetableName: String, timetableId: String): IcsExportResultDto

    @WorkerThread
    fun importIcs(request: IcsImportRequestDto, timetableId: String): IcsImportResultDto

    @WorkerThread
    fun importIcsAsNewTimetable(request: IcsImportRequestDto, sourceName: String?): IcsImportResultDto

    companion object {
        val KEY = ServiceKey<TimetableDataService>("hita.timetable.data")
    }
}
