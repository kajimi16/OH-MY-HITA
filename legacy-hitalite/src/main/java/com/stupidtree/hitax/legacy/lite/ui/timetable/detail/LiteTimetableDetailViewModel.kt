package com.stupidtree.hitax.legacy.lite.ui.timetable.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stupidtree.component.data.DataState
import com.stupidtree.component.data.MTransformations
import com.stupidtree.hitax.data.model.timetable.TeacherInfo
import com.stupidtree.hitax.data.model.timetable.TermSubject
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.legacy.lite.LiteHostBridge

/**
 * HITA_Lite 课表详情数据门面。
 *
 * 前置条件：LiteHostBridge 已注入宿主 TimetableService。
 * 后置条件：课表、科目、教师与导出结果全部来自宿主服务。
 * 不变式：不创建 Room、Retrofit、Repository 或登录单例。
 * 违约处理：服务缺失时返回空数据，界面保持可用但不写入。
 */
class LiteTimetableDetailViewModel : ViewModel() {

    private val service = LiteHostBridge.timetable
    private val timetableIdController = MutableLiveData<String>()
    private val exportController = MutableLiveData<Timetable>()

    val timetableLiveData: LiveData<Timetable> = MTransformations.switchMap(timetableIdController) { id ->
        service?.getTimetablesById(id) ?: MutableLiveData<Timetable>()
    }

    val subjectsLiveData: LiveData<List<TermSubject>> = MTransformations.switchMap(timetableIdController) { id ->
        service?.getSubjects(id) ?: MutableLiveData<List<TermSubject>>(emptyList())
    }

    val teacherInfoLiveData: LiveData<MutableList<TeacherInfo>> =
        MTransformations.switchMap(timetableIdController) { id ->
            service?.getTeachersInfo(id)
                ?: MutableLiveData<MutableList<TeacherInfo>>(mutableListOf())
        }

    val exportToICSResult: LiveData<DataState<String>> = MTransformations.switchMap(exportController) { timetable ->
        service?.exportToICS(timetable.name ?: DEFAULT_TIMETABLE_NAME, timetable.id)
            ?: MutableLiveData<DataState<String>>(DataState(DataState.STATE.FETCH_FAILED))
    }

    fun getSubjectProgress(subjectId: String): LiveData<Pair<Int, Int>> =
        service?.getProgressOfSubject(subjectId, System.currentTimeMillis())
            ?: MutableLiveData<Pair<Int, Int>>(Pair(0, 1))

    fun startRefresh(id: String) {
        if (id.isBlank()) return
        timetableIdController.value = id
    }

    fun startSaveTimetableInfo() {
        val timetable = timetableLiveData.value ?: return
        service?.actionSaveTimetable(timetable)
    }

    fun startChangeTimetableStructure(period: TimePeriodInDay, position: Int) {
        val timetable = timetableLiveData.value ?: return
        service?.actionChangeTimetableStructure(timetable, period, position)
    }

    fun startChangeTimetableStartTime(startTime: Long) {
        val timetable = timetableLiveData.value ?: return
        service?.actionChangeTimetableStartDate(timetable, startTime)
    }

    fun startResetSubjectColors() {
        val timetable = timetableLiveData.value ?: return
        service?.actionResetSubjectColors(timetable.id)
    }

    fun startDeleteSubjects(subjects: Collection<TermSubject>) {
        service?.actionDeleteSubjects(subjects.toList())
    }

    fun startChangeSubjectColor(subjectId: String, color: Int) {
        service?.actionChangeSubjectColor(subjectId, color)
    }

    fun exportToIcs() {
        val timetable = timetableLiveData.value ?: return
        exportController.value = timetable
    }

    private companion object {
        const val DEFAULT_TIMETABLE_NAME = "课表"
    }
}
