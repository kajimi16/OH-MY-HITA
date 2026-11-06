package com.stupidtree.hita.api

import androidx.lifecycle.LiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.data.model.eas.CourseSelectionCourseStatus
import com.stupidtree.hitax.data.model.eas.TermItem

data class CourseSubmitResult(
    val status: CourseSelectionCourseStatus,
    val message: String = "",
)

/** 选课提交（API 7）。调用方必须做显式二次确认。 */
interface CourseSelectionService {
    fun submit(
        term: TermItem,
        poolCode: String,
        requestId: String,
        courseId: String,
    ): LiveData<DataState<CourseSubmitResult>>

    companion object {
        val KEY = ServiceKey<CourseSelectionService>("hita.eas.courseSelection")
    }
}
