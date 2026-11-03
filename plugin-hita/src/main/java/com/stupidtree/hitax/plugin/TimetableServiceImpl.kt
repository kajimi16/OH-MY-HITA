package com.stupidtree.hitax.plugin

import androidx.lifecycle.LiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.IcsImportResult
import com.stupidtree.hitax.data.model.timetable.TeacherInfo
import com.stupidtree.hitax.data.model.timetable.TermSubject
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.data.repository.SubjectRepository
import com.stupidtree.hitax.data.repository.TimetableRepository
import java.io.InputStream

/** [TimetableService] 的实现：纯转发到两个仓库。 */
class TimetableServiceImpl(
    private val timetables: TimetableRepository,
    private val subjects: SubjectRepository,
) : TimetableService {

    override fun getTimetables(): LiveData<List<Timetable>> = timetables.getTimetables()

    override fun getTimetablesById(id: String): LiveData<Timetable> = timetables.getTimetablesById(id)

    override fun getTimetableByEasCode(code: String): LiveData<Timetable?> =
        timetables.getTimetableByEasCode(code)

    override fun getCurrentWeekOfTimetable(easCode: String?): LiveData<Int> =
        timetables.getCurrentWeekOfTimetable(easCode)

    override fun getRecentTimetable(): LiveData<Timetable?> = timetables.getRecentTimetable()

    override fun getTimetableCount(): LiveData<Int> = timetables.getTimetableCount()

    override fun getEventsDuring(from: Long, to: Long): LiveData<List<EventItem>> =
        timetables.getEventsDuring(from, to)

    override fun getEventsAfter(from: Long, limit: Int): LiveData<List<EventItem>> =
        timetables.getEventsAfter(from, limit)

    override fun getEventsDuringWithColor(from: Long, to: Long): LiveData<List<EventItem>> =
        timetables.getEventsDuringWithColor(from, to)

    override fun getTodayEventsSync(): List<EventItem> = timetables.getTodayEventsSync()

    override fun getClassesOfSubject(subjectId: String): LiveData<List<EventItem>> =
        timetables.getClassesOfSubject(subjectId)

    override fun searchLocation(str: String): LiveData<List<String>> = timetables.searchLocation(str)

    override fun getSubjects(timetableId: String): LiveData<List<TermSubject>> =
        subjects.getSubjects(timetableId)

    override fun getSubjectById(subjectId: String): LiveData<TermSubject> =
        subjects.getSubjectById(subjectId)

    override fun getSubjectColors(subjectId: String): LiveData<TermSubject> =
        subjects.getSubjectColors(subjectId)

    override fun getTeachersInfo(timetableId: String): LiveData<MutableList<TeacherInfo>> =
        subjects.getTeachersInfo(timetableId)

    override fun getTeachersOfSubject(timetableId: String, subjectId: String): LiveData<List<String>> =
        subjects.getTeachersOfSubject(timetableId, subjectId)

    override fun getProgressOfSubject(subjectId: String, ts: Long): LiveData<Pair<Int, Int>> =
        subjects.getProgressOfSubject(subjectId, ts)

    override fun actionDeleteTimetables(timetables: List<Timetable>) =
        this.timetables.actionDeleteTimetables(timetables)

    override fun actionDeleteEvents(courses: Collection<EventItem>) =
        timetables.actionDeleteEvents(courses)

    override fun actionNewTimetable() = timetables.actionNewTimetable()

    override fun actionSaveTimetable(timetable: Timetable) = timetables.actionSaveTimetable(timetable)

    override fun actionChangeTimetableStartDate(timetable: Timetable, startTime: Long) =
        timetables.actionChangeTimetableStartDate(timetable, startTime)

    override fun actionChangeTimetableStructure(
        timetable: Timetable,
        tp: TimePeriodInDay,
        position: Int,
    ) = timetables.actionChangeTimetableStructure(timetable, tp, position)

    override fun actionAddEvents(data: List<EventItem>) = timetables.actionAddEvents(data)

    override fun actionClearData() = timetables.actionClearData()

    override fun actionSaveSubjectInfo(subject: TermSubject) = subjects.actionSaveSubjectInfo(subject)

    override fun actionResetRecentSubjectColors() = subjects.actionResetRecentSubjectColors()

    override fun actionResetSubjectColors(timetableId: String) =
        subjects.actionResetSubjectColors(timetableId)

    override fun actionDeleteSubjects(subjects: List<TermSubject>) =
        this.subjects.actionDeleteSubjects(subjects)

    override fun actionChangeSubjectColor(subjectId: String, color: Int) =
        subjects.actionChangeSubjectColor(subjectId, color)

    override fun exportToICS(timetableName: String, timetableId: String): LiveData<DataState<String>> =
        timetables.exportToICS(timetableName, timetableId)

    override fun importFromICS(inputStream: InputStream, timetableId: String): LiveData<DataState<Int>> =
        timetables.importFromICS(inputStream, timetableId)

    override fun importFromICSAsNewTimetable(
        inputStream: InputStream,
        sourceName: String?,
    ): LiveData<DataState<IcsImportResult>> =
        timetables.importFromICSAsNewTimetable(inputStream, sourceName)
}
