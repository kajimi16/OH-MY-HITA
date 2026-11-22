package com.stupidtree.hitax.legacy.lite.ui.event

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stupidtree.component.data.MTransformations
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.legacy.lite.LiteHostBridge

/**
 * 事件详情数据门面。
 *
 * 前置条件：LiteHostBridge 已注入宿主 TimetableService。
 * 后置条件：进度与删除动作都交给宿主课表服务。
 * 违约处理：服务缺失时进度固定为 0/1，删除不写入。
 */
class LiteEventItemViewModel : ViewModel() {

    private val service = LiteHostBridge.timetable

    val eventItemLiveData = MutableLiveData<EventItem>()

    val progressLiveData: LiveData<Pair<Int, Int>> = MTransformations.switchMap(eventItemLiveData) { event ->
        service?.getProgressOfSubject(event.subjectId, event.from.time)
            ?: MutableLiveData<Pair<Int, Int>>(Pair(0, 1))
    }

    fun delete() {
        val event = eventItemLiveData.value ?: return
        service?.actionDeleteEvents(listOf(event))
    }
}
