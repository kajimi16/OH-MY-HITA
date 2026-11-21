package com.stupidtree.hitax.legacy.lite.ui.navigation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.legacy.lite.LiteHostBridge

/**
 * 功能中心只读数据。
 *
 * 前置条件：LiteHostBridge 已注入宿主 TimetableService；缺失时回落到空值。
 * 后置条件：最近课表与课表数量来自宿主，不创建第二份数据库。
 */
class LiteNavigationViewModel : ViewModel() {
    private val service = LiteHostBridge.timetable

    val recentTimetableLiveData: LiveData<Timetable?> =
        service?.getRecentTimetable() ?: MutableLiveData(null)

    val timetableCountLiveData: LiveData<Int> =
        service?.getTimetableCount() ?: MutableLiveData(0)

    val unreadMessageLiveData: LiveData<Int> = MutableLiveData(0)
}
