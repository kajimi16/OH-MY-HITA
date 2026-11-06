package com.stupidtree.hitax.eas

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.CourseSelectionService
import com.stupidtree.hita.api.CourseSubmitResult
import com.stupidtree.hitax.data.model.eas.CourseSelectionCourseStatus
import com.stupidtree.hitax.data.model.eas.TermItem
import com.stupidtree.hitax.data.repository.EASRepository
import com.stupidtree.hitax.data.source.web.eas.EASource
import com.stupidtree.hitax.data.source.web.eas.ShenzhenCourseSelectionForm
import com.stupidtree.hitax.data.source.web.eas.ShenzhenCourseSelectionResponseParser

internal class CourseSelectionServiceImpl(private val application: Application) : CourseSelectionService {
    private val repo get() = EASRepository.getInstance(application)
    private val source = EASource()

    override fun submit(
        term: TermItem,
        poolCode: String,
        requestId: String,
        courseId: String,
    ): LiveData<DataState<CourseSubmitResult>> {
        val result = MutableLiveData<DataState<CourseSubmitResult>>()
        Thread {
            val token = repo.getEasToken()
            if (token.cookies.isEmpty()) {
                result.postValue(DataState(DataState.STATE.NOT_LOGGED_IN, "请先连接深圳 Web 教务"))
                return@Thread
            }
            runCatching {
                require(poolCode.isNotBlank()) { "缺少课程池" }
                require(requestId.isNotBlank()) { "课程缺少选课请求 id" }
                val form = ShenzhenCourseSelectionForm.build(
                    studentType = token.getStudentType(),
                    termYearCode = term.yearCode,
                    termCode = term.termCode,
                    poolCode = poolCode,
                    requestId = requestId,
                )
                val response = source.jwPost(
                    token, "/Xsxk/addGouwuche", form, "/Xsxk/query/1",
                )
                val parsed = ShenzhenCourseSelectionResponseParser.parse(
                    response.statusCode(), response.url().toString(), response.body(),
                )
                if (parsed.status == CourseSelectionCourseStatus.AUTH_REQUIRED) {
                    DataState(DataState.STATE.NOT_LOGGED_IN, "深圳 Web 会话已失效")
                } else {
                    DataState(CourseSubmitResult(parsed.status, parsed.message), DataState.STATE.SUCCESS)
                }
            }.onSuccess { result.postValue(it) }
                .onFailure { result.postValue(DataState(DataState.STATE.FETCH_FAILED, it.message)) }
        }.start()
        return result
    }
}
