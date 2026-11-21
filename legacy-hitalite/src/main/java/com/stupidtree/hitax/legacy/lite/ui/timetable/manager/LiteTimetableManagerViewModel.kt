package com.stupidtree.hitax.legacy.lite.ui.timetable.manager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.legacy.lite.LiteHostBridge

/**
 * 课表管理只读/写入门面。
 *
 * 前置条件：宿主 TimetableService 已注入；缺失时返回空列表并拒绝写入。
 * 后置条件：所有变更提交给宿主服务，不创建 Lite 旧 Room。
 */
class LiteTimetableManagerViewModel : ViewModel() {
    private val service = LiteHostBridge.timetable

    val timetablesLiveData: LiveData<List<Timetable>> =
        service?.getTimetables() ?: MutableLiveData(emptyList())

    fun startDeleteTimetables(timetables: List<Timetable>) {
        service?.actionDeleteTimetables(timetables)
    }

    fun startNewTimetable() {
        service?.actionNewTimetable()
    }
}
