package com.stupidtree.hita.api

import androidx.lifecycle.LiveData
import com.stupidtree.component.data.DataState

/** 一栋教学楼。 */
data class ClassroomBuilding(
    val id: String,
    val name: String,
)

/** 教室在某一周内被占用的一个课时。 */
data class ClassroomSlot(
    val dayOfWeek: Int,
    val period: Int,
)

/** 一间教室及其占用时段；未占用的时段即可用。 */
data class ClassroomRoom(
    val id: String,
    val name: String,
    val capacity: Int = 0,
    val occupied: List<ClassroomSlot> = emptyList(),
)

/**
 * 教务空教室查询（API 7）。
 *
 * 由 `plugin-eas` 提供；未登录时返回 NOT_LOGGED_IN，教学楼或学期缺失时返回空。
 */
interface EmptyClassroomService {

    fun buildings(): LiveData<DataState<List<ClassroomBuilding>>>

    /** [week] 为教学周，从 1 开始。 */
    fun query(buildingId: String, week: Int = 1): LiveData<DataState<List<ClassroomRoom>>>

    companion object {
        val KEY = ServiceKey<EmptyClassroomService>("hita.eas.classrooms")
    }
}
