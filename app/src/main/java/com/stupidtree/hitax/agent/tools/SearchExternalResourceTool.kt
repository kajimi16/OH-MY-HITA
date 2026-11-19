package com.stupidtree.hitax.agent.tools

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.gson.JsonObject
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.ResourceDataService
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.agent.model.AgentResourceCard
import com.stupidtree.hitax.data.model.resource.CourseResourceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** 通过宿主 ResourceDataService 搜 HOA 课程资源；插件缺席时返回明确错误。 */
class SearchExternalResourceTool : SuspendingReActTool {
    override suspend fun execute(input: ReActToolInput): String {
        val keyword = extractKeyword(input.arguments)
            ?: input.actionInput.trim().removeSurrounding("{", "}").trim('"')
        if (keyword.isBlank()) return "资料搜索失败: 无法解析查询条件"
        val service = (input.application as HApplication).pluginHost.service(ResourceDataService.KEY)
            ?: return "资料搜索失败: 课程资源插件未安装"
        val state = service.searchCourses(keyword).await()
        val results = state.data.orEmpty()
        if (state.state != DataState.STATE.SUCCESS || results.isEmpty()) {
            return "未在 HOA 找到“$keyword”相关资料"
        }
        input.onResourceCards(results.take(10).map { it.toCard(keyword) })
        val formatted = results.take(10).map { item ->
            buildString {
                append("[HOA] ")
                append(item.courseName.ifBlank { item.courseCode.ifBlank { item.repoName } })
                if (item.courseCode.isNotBlank()) append(" · ${item.courseCode}")
                val teachers = item.teachers.take(2).joinToString(" / ")
                if (teachers.isNotBlank()) append(" · 教师: $teachers")
            }
        }
        return buildString {
            append("在 HOA 找到 ${results.size} 个课程资源。")
            append("\n")
            append(formatted.mapIndexed { index, value -> "${index + 1}. $value" }.joinToString("\n"))
            append("\n\n已在回答下方生成可点击资料卡片。")
        }
    }

    internal fun extractKeyword(arguments: JsonObject): String? =
        arguments.stringArg("query", "keyword", "course_name", "param")

    private fun CourseResourceItem.toCard(keyword: String) = AgentResourceCard(
        title = courseName.ifBlank { courseCode.ifBlank { repoName } },
        subtitle = listOf(courseCode, teachers.take(2).joinToString(" / ")).filter { it.isNotBlank() }.joinToString(" · "),
        sourceTag = "HOA",
        source = "HOA",
        path = repoName,
        query = keyword,
        repoName = repoName,
        courseCode = courseCode,
        repoType = repoType.ifBlank { "normal" },
    )

    private suspend fun <T> LiveData<T>.await(): T = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { cont ->
            val observer = object : Observer<T> {
                override fun onChanged(value: T) {
                    removeObserver(this)
                    if (cont.isActive) cont.resume(value)
                }
            }
            observeForever(observer)
            cont.invokeOnCancellation { removeObserver(observer) }
        }
    }
}
