package com.stupidtree.hita.api

import androidx.lifecycle.LiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.data.model.eas.TermItem
import com.stupidtree.hitax.data.model.timetable.TermSubject

/**
 * 深圳教务课程池（API 7，增量契约）。
 * 只读课程列表；选课/附件/历史挂科率仍由后续版本扩展。
 */
data class CatalogAttachment(
    val name: String,
    val path: String,
    val kind: String,
)

interface CourseCatalogService {
    fun terms(): LiveData<DataState<List<TermItem>>>

    fun courses(
        term: TermItem,
        poolCode: String = "bx-b-b",
        page: Int = 1,
        pageSize: Int = 20,
        keyword: String = "",
    ): LiveData<DataState<List<TermSubject>>>

    /** 加入选课（实际选课动作）。 */
    fun selectCourse(
        term: TermItem,
        course: TermSubject,
        poolCode: String = "bx-b-b",
    ): LiveData<DataState<String>>

    fun attachments(course: TermSubject): LiveData<DataState<List<CatalogAttachment>>>

    companion object {
        val KEY = ServiceKey<CourseCatalogService>("hita.eas.courseCatalog")
    }
}
