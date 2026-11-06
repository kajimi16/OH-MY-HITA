package com.stupidtree.hitax.plugin

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.TeacherSearchResult
import com.stupidtree.hita.api.TeacherService
import com.stupidtree.hitax.data.repository.TeacherInfoRepository

internal class TeacherServiceImpl(application: Application) : TeacherService {
    private val repo = TeacherInfoRepository.getInstance(application)

    override fun search(query: String): LiveData<DataState<List<TeacherSearchResult>>> =
        repo.searchTeachers(query).map { state ->
            if (state.state != DataState.STATE.SUCCESS) {
                DataState<List<TeacherSearchResult>>(state.state).apply { message = state.message }
            } else {
                DataState(
                    state.data.orEmpty().map {
                        TeacherSearchResult(
                            name = it.name,
                            department = it.department,
                            courseName = it.courseName,
                            courseCode = it.courseCode,
                        )
                    },
                    DataState.STATE.SUCCESS,
                )
            }
        }
}
