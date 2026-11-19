package com.stupidtree.hitax.plugin

import androidx.lifecycle.LiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.ResourceDataService
import com.stupidtree.hitax.data.model.resource.CourseReadmeData
import com.stupidtree.hitax.data.model.resource.CourseResourceItem
import com.stupidtree.hitax.data.model.resource.CourseStructureSummary
import com.stupidtree.hitax.data.model.resource.ValidateReadmeResult
import com.stupidtree.hitax.data.repository.HoaRepository
import org.json.JSONArray

/** 把 HOA 仓库暴露成跨插件契约；网络与解析逻辑仍留在本插件内。 */
class ResourceDataServiceImpl(
    private val repository: HoaRepository = HoaRepository.getInstance(),
) : ResourceDataService {

    override fun searchCourses(query: String): LiveData<DataState<List<CourseResourceItem>>> =
        repository.searchCourses(query)

    override fun getCourseReadme(repoName: String): LiveData<DataState<CourseReadmeData>> =
        repository.getCourseReadme(repoName)

    override fun getCourseStructure(repoName: String): LiveData<DataState<CourseStructureSummary>> =
        repository.getCourseStructure(repoName)

    override fun validateReadme(
        repoName: String,
        courseCode: String,
        courseName: String,
        repoType: String,
        readmeMarkdown: String,
    ): LiveData<DataState<ValidateReadmeResult>> =
        repository.validateReadme(repoName, courseCode, courseName, repoType, readmeMarkdown)

    override fun submitOps(
        repoName: String,
        courseCode: String,
        courseName: String,
        repoType: String,
        opsJson: String,
    ): LiveData<DataState<String>> =
        repository.submitOps(repoName, courseCode, courseName, repoType, JSONArray(opsJson))
}
