package com.stupidtree.hitax.eas

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.ClassroomBuilding
import com.stupidtree.hita.api.ClassroomRoom
import com.stupidtree.hita.api.ClassroomSlot
import com.stupidtree.hita.api.EmptyClassroomService
import com.stupidtree.hitax.data.model.eas.TermItem
import com.stupidtree.hitax.data.repository.EASRepository
import com.stupidtree.hitax.ui.eas.classroom.BuildingItem
import com.stupidtree.hitax.ui.eas.classroom.ClassroomItem

/** 把教务仓库的教学楼/空教室数据转成稳定的插件 API 模型。 */
class EmptyClassroomServiceImpl(application: Application) : EmptyClassroomService {

    private val repository = EASRepository.getInstance(application)

    override fun buildings(): LiveData<DataState<List<ClassroomBuilding>>> {
        val out = MediatorLiveData<DataState<List<ClassroomBuilding>>>()
        out.addSource(repository.getTeachingBuildings()) { state ->
            out.value = when (state.state) {
                DataState.STATE.SUCCESS -> DataState(
                    state.data.orEmpty().map { ClassroomBuilding(it.id, it.name.orEmpty()) },
                    state.state,
                )
                else -> DataState(state.state, state.message)
            }
        }
        return out
    }

    override fun query(buildingId: String, week: Int): LiveData<DataState<List<ClassroomRoom>>> {
        val out = MediatorLiveData<DataState<List<ClassroomRoom>>>()
        val terms = repository.getAllTerms()
        out.addSource(terms) { termState ->
            val term = termState.data?.firstOrNull { it.isCurrent }
                ?: termState.data?.firstOrNull()
            if (termState.state != DataState.STATE.SUCCESS || term == null) {
                if (termState.state != DataState.STATE.SUCCESS) {
                    out.value = DataState(termState.state, termState.message)
                }
                out.removeSource(terms)
            } else {
                out.removeSource(terms)
                loadBuilding(out, term, buildingId, week.coerceAtLeast(1))
            }
        }
        return out
    }

    private fun loadBuilding(
        out: MediatorLiveData<DataState<List<ClassroomRoom>>>,
        term: TermItem,
        buildingId: String,
        week: Int,
    ) {
        val buildings = repository.getTeachingBuildings()
        out.addSource(buildings) { state ->
            val building = state.data?.firstOrNull { it.id == buildingId || it.name == buildingId }
            if (state.state != DataState.STATE.SUCCESS || building == null) {
                if (state.state != DataState.STATE.SUCCESS) {
                    out.value = DataState(state.state, state.message)
                } else {
                    out.value = DataState(DataState.STATE.NOT_EXIST, "教学楼不存在")
                }
                out.removeSource(buildings)
            } else {
                out.removeSource(buildings)
                val query = repository.queryEmptyClassroom(term, building, week)
                out.addSource(query) { result ->
                    out.value = when (result.state) {
                        DataState.STATE.SUCCESS -> DataState(
                            result.data.orEmpty().map { it.toApi() },
                            result.state,
                        )
                        else -> DataState(result.state, result.message)
                    }
                }
            }
        }
    }

    private fun ClassroomItem.toApi(): ClassroomRoom = ClassroomRoom(
        id = id.ifBlank { name },
        name = name,
        capacity = capacity,
        occupied = scheduleList.mapNotNull { slot ->
            val day = slot.optInt("XQJ", 0)
            val period = slot.optInt("XJ", 0)
            if (day in 1..7 && period > 0) ClassroomSlot(day, period) else null
        }.distinct(),
    )

    @Suppress("unused")
    private fun BuildingItem.toApi(): ClassroomBuilding = ClassroomBuilding(id, name.orEmpty())
}
