package com.stupidtree.hitax.agent.tools

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.gson.JsonObject
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.TeacherService
import com.stupidtree.hitax.HApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SearchTeacherTool : SuspendingReActTool {
    override suspend fun execute(input: ReActToolInput): String {
        val query = extractQuery(input.arguments)
            ?: input.actionInput.trim().removeSurrounding("{", "}")
        if (query.isBlank()) return "教师搜索失败：缺少教师姓名"
        val service = (input.application as HApplication).pluginHost
            .service(TeacherService.KEY)
            ?: return "教师服务不可用"
        val state = service.search(query).await(20_000)
            ?: return "教师搜索超时"
        if (state.state != DataState.STATE.SUCCESS) {
            return "教师搜索失败：${state.message ?: state.state}"
        }
        val results = state.data.orEmpty()
        if (results.isEmpty()) return "没有找到教师「$query」"
        return buildString {
            append("教师搜索结果：\n")
            results.take(20).forEach { teacher ->
                append("- ${teacher.name}")
                if (teacher.department.isNotBlank()) append("（${teacher.department}）")
                if (teacher.courseName.isNotBlank()) append("：${teacher.courseName}")
                append('\n')
            }
        }.trimEnd()
    }

    internal fun extractQuery(arguments: JsonObject): String? =
        arguments.stringArg("name", "teacher_name")

    private suspend fun <T> LiveData<T>.await(timeoutMs: Long): T? =
        withContext(Dispatchers.Main) {
            val latch = CountDownLatch(1)
            var value: T? = null
            val observer = object : Observer<T> {
                override fun onChanged(result: T) {
                    value = result
                    removeObserver(this)
                    latch.countDown()
                }
            }
            observeForever(observer)
            try {
                withContext(Dispatchers.IO) { latch.await(timeoutMs, TimeUnit.MILLISECONDS) }
                value
            } finally {
                removeObserver(observer)
            }
        }
}
