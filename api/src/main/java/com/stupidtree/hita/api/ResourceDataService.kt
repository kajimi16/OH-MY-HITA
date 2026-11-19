package com.stupidtree.hita.api

import androidx.lifecycle.LiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.data.model.resource.CourseReadmeData
import com.stupidtree.hitax.data.model.resource.CourseResourceItem
import com.stupidtree.hitax.data.model.resource.CourseStructureSummary
import com.stupidtree.hitax.data.model.resource.ValidateReadmeResult

/**
 * 课程资源数据契约。
 *
 * 数据抓取（HOA REST + 解析）留在构建期 `:plugin-resource` 内，
 * 运行时界面插件只通过 [PluginUiHost.service] 取这个服务，不复制网络层。
 *
 * 所有方法都返回 LiveData：调用方（宿主或插件）不关心线程与重试。
 */
interface ResourceDataService {
    fun searchCourses(query: String): LiveData<DataState<List<CourseResourceItem>>>

    fun getCourseReadme(repoName: String): LiveData<DataState<CourseReadmeData>>

    fun getCourseStructure(repoName: String): LiveData<DataState<CourseStructureSummary>>

    fun validateReadme(
        repoName: String,
        courseCode: String,
        courseName: String,
        repoType: String,
        readmeMarkdown: String,
    ): LiveData<DataState<ValidateReadmeResult>>

    /** [opsJson] 是 JSON 数组文本，避免把 org.json 类型带进契约。 */
    fun submitOps(
        repoName: String,
        courseCode: String,
        courseName: String,
        repoType: String,
        opsJson: String,
    ): LiveData<DataState<String>>

    companion object {
        val KEY = ServiceKey<ResourceDataService>("hita.resource.data")
    }
}
