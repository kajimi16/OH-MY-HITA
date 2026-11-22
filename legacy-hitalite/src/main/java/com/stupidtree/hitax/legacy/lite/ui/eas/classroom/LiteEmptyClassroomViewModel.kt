package com.stupidtree.hitax.legacy.lite.ui.eas.classroom

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stupidtree.component.data.DataState
import com.stupidtree.component.data.MTransformations
import com.stupidtree.hita.api.ClassroomBuilding
import com.stupidtree.hita.api.ClassroomRoom
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.legacy.lite.LiteHostBridge

/**
 * HITA_Lite 空教室查询门面。
 *
 * 前置条件：宿主已注入 EmptyClassroomService；未注入时返回失败态，不创建第二套数据源。
 * 后置条件：教学楼与教室数据全部来自宿主服务，学期由宿主服务按当前学期选定。
 * 不变式：同一时刻只观察一个查询结果；每次刷新换一个新的查询 LiveData。
 */
class LiteEmptyClassroomViewModel : ViewModel() {

    private val classroomService = LiteHostBridge.classrooms
    private val recentTimetable: LiveData<Timetable?> =
        LiteHostBridge.timetable?.getRecentTimetable() ?: MutableLiveData<Timetable?>(null)

    private val queryController = MutableLiveData<Query>()

    val buildingsLiveData: LiveData<DataState<List<ClassroomBuilding>>> =
        classroomService?.buildings() ?: MutableLiveData(
            DataState<List<ClassroomBuilding>>(
                DataState.STATE.FETCH_FAILED,
                "empty classroom service missing"
            )
        )

    val roomsLiveData: LiveData<DataState<List<ClassroomRoom>>> =
        MTransformations.switchMap(queryController) { query ->
            classroomService?.query(query.buildingId, query.week)
                ?: MutableLiveData(
                    DataState<List<ClassroomRoom>>(
                        DataState.STATE.FETCH_FAILED,
                        "empty classroom service missing"
                    )
                )
        }

    val selectedBuildingLiveData = MutableLiveData<ClassroomBuilding>()
    val selectedWeekLiveData = MutableLiveData<Int>()

    val currentWeekLiveData: LiveData<Int> = MTransformations.map(recentTimetable) { timetable ->
        val week = timetable?.getWeekNumber(System.currentTimeMillis()) ?: -1
        if (week > 0) week else 1
    }

    /** 课表结构快照，用于计算教室当前占用状态。 */
    fun structure(): List<TimePeriodInDay> =
        recentTimetable.value?.scheduleStructure.orEmpty()

    /** 当前课表名称；宿主没有课表时为 null。 */
    fun timetableName(): String? = recentTimetable.value?.name

    fun selectBuilding(building: ClassroomBuilding) {
        selectedBuildingLiveData.value = building
        startRefresh()
    }

    fun selectWeek(week: Int) {
        selectedWeekLiveData.value = week
        startRefresh()
    }

    /** 用当前选中的教学楼与周次发起查询；缺任一项时不发请求。 */
    fun startRefresh() {
        val building = selectedBuildingLiveData.value ?: return
        val week = selectedWeekLiveData.value ?: 1
        queryController.value = Query(building.id, week)
    }

    private data class Query(val buildingId: String, val week: Int)
}
