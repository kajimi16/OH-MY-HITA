package com.stupidtree.hitax.eas

import android.app.Application
import androidx.lifecycle.LiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.CourseCatalogService
import com.stupidtree.hita.api.CatalogAttachment
import com.stupidtree.hitax.data.model.eas.TermItem
import com.stupidtree.hitax.data.model.timetable.TermSubject
import com.stupidtree.hitax.data.repository.EASRepository

class CourseCatalogServiceImpl(private val application: Application) : CourseCatalogService {
    private val repo get() = EASRepository.getInstance(application)

    override fun terms(): LiveData<DataState<List<TermItem>>> = repo.getAllTerms()

    override fun courses(
        term: TermItem,
        poolCode: String,
        page: Int,
        pageSize: Int,
        keyword: String,
    ): LiveData<DataState<List<TermSubject>>> =
        repo.getAvailableCourses(term, poolCode, page, pageSize, keyword)

    override fun selectCourse(
        term: TermItem,
        course: TermSubject,
        poolCode: String,
    ): LiveData<DataState<String>> =
        repo.selectCourse(term, course.key.orEmpty(), poolCode)

    override fun attachments(course: TermSubject): LiveData<DataState<List<CatalogAttachment>>> =
        repo.getCourseAttachments(
            course.externalCourseId.orEmpty(),
            course.taskNumber.orEmpty(),
        )

}
