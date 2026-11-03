package com.stupidtree.hita.api

import androidx.lifecycle.LiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.IcsImportResult
import com.stupidtree.hitax.data.model.timetable.TeacherInfo
import com.stupidtree.hitax.data.model.timetable.TermSubject
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.data.model.timetable.Timetable
import java.io.InputStream

/**
 * 课表能力契约。
 *
 * 宿主（UI / Worker / Widget）只依赖这个接口；实现、Room、DAO、ICS 解析都在
 * `:plugin-hita` 里。插件缺席时宿主取不到服务，相关入口失效但不会编译失败。
 *
 * 模型类型来自 `:model`，宿主与插件共享同一份定义；接口本身不持有实现。
 */
interface TimetableService {

    // ---- 读 ----

    fun getTimetables(): LiveData<List<Timetable>>

    fun getTimetablesById(id: String): LiveData<Timetable>

    fun getTimetableByEasCode(code: String): LiveData<Timetable?>

    fun getCurrentWeekOfTimetable(easCode: String?): LiveData<Int>

    fun getRecentTimetable(): LiveData<Timetable?>

    fun getTimetableCount(): LiveData<Int>

    fun getEventsDuring(from: Long, to: Long): LiveData<List<EventItem>>

    fun getEventsAfter(from: Long, limit: Int): LiveData<List<EventItem>>

    fun getEventsDuringWithColor(from: Long, to: Long): LiveData<List<EventItem>>

    fun getTodayEventsSync(): List<EventItem>

    fun getClassesOfSubject(subjectId: String): LiveData<List<EventItem>>

    fun searchLocation(str: String): LiveData<List<String>>

    fun getSubjects(timetableId: String): LiveData<List<TermSubject>>

    fun getSubjectById(subjectId: String): LiveData<TermSubject>

    fun getSubjectColors(subjectId: String): LiveData<TermSubject>

    fun getTeachersInfo(timetableId: String): LiveData<MutableList<TeacherInfo>>

    fun getTeachersOfSubject(timetableId: String, subjectId: String): LiveData<List<String>>

    fun getProgressOfSubject(subjectId: String, ts: Long): LiveData<Pair<Int, Int>>

    // ---- 写 ----

    fun actionDeleteTimetables(timetables: List<Timetable>)

    fun actionDeleteEvents(courses: Collection<EventItem>)

    fun actionNewTimetable()

    fun actionSaveTimetable(timetable: Timetable)

    fun actionChangeTimetableStartDate(timetable: Timetable, startTime: Long)

    fun actionChangeTimetableStructure(timetable: Timetable, tp: TimePeriodInDay, position: Int)

    fun actionAddEvents(data: List<EventItem>)

    fun actionClearData()

    fun actionSaveSubjectInfo(subject: TermSubject)

    fun actionResetRecentSubjectColors()

    fun actionResetSubjectColors(timetableId: String)

    fun actionDeleteSubjects(subjects: List<TermSubject>)

    fun actionChangeSubjectColor(subjectId: String, color: Int)

    // ---- ICS ----

    fun exportToICS(timetableName: String, timetableId: String): LiveData<DataState<String>>

    fun importFromICS(inputStream: InputStream, timetableId: String): LiveData<DataState<Int>>

    fun importFromICSAsNewTimetable(
        inputStream: InputStream,
        sourceName: String?,
    ): LiveData<DataState<IcsImportResult>>

    companion object {
        val KEY = ServiceKey<TimetableService>("hita.timetable")
    }
}
