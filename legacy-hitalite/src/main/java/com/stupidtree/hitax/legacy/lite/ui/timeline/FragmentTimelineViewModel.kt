package com.stupidtree.hitax.legacy.lite.ui.timeline

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.legacy.lite.LiteHostBridge
import java.util.Calendar

/**
 * 今日页数据源。
 *
 * 所有数据经 [LiteHostBridge.timetable] 取得；服务缺席时不建库、不发网络请求，
 * 直接以空列表呈现空态。
 */
class FragmentTimelineViewModel : ViewModel() {

    private val refreshTrigger = MutableLiveData(0)

    private val todayEventsController = MediatorLiveData<List<EventItem>>()
    private val upcomingEventsController = MediatorLiveData<List<EventItem>>()

    val todayEvents: LiveData<List<EventItem>> = todayEventsController
    val upcomingEvents: LiveData<List<EventItem>> = upcomingEventsController

    private var todayQuery: LiveData<List<EventItem>>? = null
    private var upcomingQuery: LiveData<List<EventItem>>? = null

    init {
        todayEventsController.addSource(refreshTrigger) { loadToday() }
        upcomingEventsController.addSource(refreshTrigger) { loadUpcoming() }
    }

    fun refresh() {
        refreshTrigger.value = (refreshTrigger.value ?: 0) + 1
    }

    private fun loadToday() {
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val query = LiteHostBridge.timetable?.getEventsDuring(start, start + DAY_MILLS)
            ?: MutableLiveData<List<EventItem>>(emptyList())
        todayQuery?.let(todayEventsController::removeSource)
        todayQuery = query
        todayEventsController.addSource(query) { events ->
            todayEventsController.value = events.sortedBy { it.from.time }
        }
    }

    private fun loadUpcoming() {
        val query = LiteHostBridge.timetable?.getEventsAfter(System.currentTimeMillis(), UPCOMING_LIMIT)
            ?: MutableLiveData<List<EventItem>>(emptyList())
        upcomingQuery?.let(upcomingEventsController::removeSource)
        upcomingQuery = query
        upcomingEventsController.addSource(query) { events ->
            upcomingEventsController.value = events.sortedBy { it.from.time }
        }
    }

    override fun onCleared() {
        todayQuery?.let(todayEventsController::removeSource)
        upcomingQuery?.let(upcomingEventsController::removeSource)
        todayQuery = null
        upcomingQuery = null
        super.onCleared()
    }

    private companion object {
        const val DAY_MILLS = 24L * 60 * 60 * 1000
        const val UPCOMING_LIMIT = 4
    }
}
