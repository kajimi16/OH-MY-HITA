package com.stupidtree.hitax.legacy.lite.ui.event.add

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.TermSubject
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.legacy.lite.LiteHostBridge
import java.sql.Timestamp
import java.util.Calendar

/**
 * 添加事件弹窗的数据门面。
 *
 * 前置条件：LiteHostBridge 已注入宿主 TimetableService。
 * 后置条件：新增科目与事件通过宿主服务写入；完成状态由四个输入共同决定。
 * 违约处理：服务缺失时保持未完成状态，不创建任何本地仓库。
 */
class LiteAddEventViewModel : ViewModel() {
    private val service = LiteHostBridge.timetable

    val timetableLiveData = MutableLiveData<DataState<Timetable>>()
    val subjectLiveData = MediatorLiveData<DataState<TermSubject>>()
    val timeRangeLiveDate = MediatorLiveData<DataState<LiteCourseTime>>()
    val nameLiveData = MediatorLiveData<String?>()
    val locationLiveData = MediatorLiveData<DataState<String>>()
    val teacherLiveData = MediatorLiveData<DataState<String>>()
    val doneLiveData = MediatorLiveData<Boolean>()

    var addSubject: Boolean = false
    var initSubject: TermSubject? = null
    var initCourseT: LiteCourseTime? = null

    init {
        doneLiveData.addSource(subjectLiveData) { checkDone() }
        doneLiveData.addSource(nameLiveData) { checkDone() }
        doneLiveData.addSource(timetableLiveData) { checkDone() }
        doneLiveData.addSource(timeRangeLiveDate) { checkDone() }

        timeRangeLiveDate.addSource(timetableLiveData) { timetableState ->
            if (timetableState.state == DataState.STATE.SUCCESS) {
                val initial = initCourseT
                if (initial != null) {
                    timeRangeLiveDate.value = DataState(initial)
                    initCourseT = null
                } else {
                    timeRangeLiveDate.value = DataState(DataState.STATE.NOTHING)
                }
            } else {
                timeRangeLiveDate.value = DataState(DataState.STATE.FETCH_FAILED)
            }
        }

        subjectLiveData.addSource(timeRangeLiveDate) { timeState ->
            if (timeState.state == DataState.STATE.SUCCESS) {
                val initial = initSubject
                if (addSubject) {
                    subjectLiveData.value = DataState(DataState.STATE.SPECIAL)
                } else if (initial != null) {
                    subjectLiveData.value = DataState(initial)
                    initSubject = null
                } else if (subjectLiveData.value?.state != DataState.STATE.SUCCESS ||
                    subjectLiveData.value?.data?.timetableId !=
                    timetableLiveData.value?.data?.id
                ) {
                    subjectLiveData.value = DataState(DataState.STATE.NOTHING)
                }
            } else {
                subjectLiveData.value = DataState(DataState.STATE.FETCH_FAILED)
            }
        }

        teacherLiveData.addSource(subjectLiveData) { subjectState ->
            if (subjectState.state == DataState.STATE.SUCCESS ||
                subjectState.state == DataState.STATE.SPECIAL
            ) {
                if (teacherLiveData.value?.state != DataState.STATE.SUCCESS) {
                    teacherLiveData.value = DataState(DataState.STATE.NOTHING)
                }
            } else {
                teacherLiveData.value = DataState(DataState.STATE.FETCH_FAILED)
            }
        }
        locationLiveData.addSource(subjectLiveData) { subjectState ->
            if (subjectState.state == DataState.STATE.SUCCESS ||
                subjectState.state == DataState.STATE.SPECIAL
            ) {
                if (locationLiveData.value?.state != DataState.STATE.SUCCESS) {
                    locationLiveData.value = DataState(DataState.STATE.NOTHING)
                }
            } else {
                locationLiveData.value = DataState(DataState.STATE.FETCH_FAILED)
            }
        }
    }
    private fun checkDone() {
        val complete = timetableLiveData.value?.state == DataState.STATE.SUCCESS &&
            (subjectLiveData.value?.state == DataState.STATE.SUCCESS ||
                subjectLiveData.value?.state == DataState.STATE.SPECIAL) &&
            timeRangeLiveDate.value?.state == DataState.STATE.SUCCESS &&
            !nameLiveData.value.isNullOrEmpty()
        doneLiveData.value = complete
    }

    fun init(
        addSubject: Boolean,
        timetable: Timetable?,
        subject: TermSubject?,
        courseTime: LiteCourseTime?,
    ) {
        timetableLiveData.value =
            if (timetable == null) {
                DataState(DataState.STATE.NOTHING)
            } else {
                DataState(timetable)
            }
        initCourseT = courseTime
        initSubject = subject
        this.addSubject = addSubject
    }
    fun createEvent() {
        val timetable = timetableLiveData.value?.data ?: return
        val range = timeRangeLiveDate.value?.data ?: return
        val subject = if (addSubject) {
            TermSubject().apply {
                name = nameLiveData.value ?: ""
                timetableId = timetable.id
            }.also { service?.actionSaveSubjectInfo(it) }
        } else {
            subjectLiveData.value?.data ?: return
        }

        var maxEndTime = 0L
        val events = mutableListOf<EventItem>()
        for (week in range.weeks) {
            val timestamps = timetable.getTimestamps(week, range.dow, range.period)
            val event = EventItem().apply {
                type = EventItem.TYPE.CLASS
                name = nameLiveData.value ?: ""
                timetableId = timetable.id
                subjectId = subject.id
                place = locationLiveData.value?.data ?: ""
                teacher = teacherLiveData.value?.data ?: ""
                from = Timestamp(timestamps[0])
                to = Timestamp(timestamps[1])
            }
            maxEndTime = maxEndTime.coerceAtLeast(event.to.time)
            events.add(event)
        }

        service?.actionAddEvents(events)
        if (maxEndTime > timetable.endTime.time) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = maxEndTime
            calendar.firstDayOfWeek = Calendar.MONDAY
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            timetable.endTime.time = calendar.timeInMillis
            service?.actionSaveTimetable(timetable)
        }
    }
}
