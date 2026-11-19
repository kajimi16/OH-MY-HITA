package com.stupidtree.hita.api

import androidx.lifecycle.LiveData
import com.stupidtree.component.data.DataState

/** 教师检索结果（API 7）。 */
data class TeacherSearchResult(
    val name: String,
    val department: String,
    val courseName: String,
    val courseCode: String,
)

interface TeacherService {
    fun search(query: String): LiveData<DataState<List<TeacherSearchResult>>>

    companion object {
        val KEY = ServiceKey<TeacherService>("hita.resource.teachers")
    }
}
