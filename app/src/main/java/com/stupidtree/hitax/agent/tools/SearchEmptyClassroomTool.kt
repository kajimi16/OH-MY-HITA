package com.stupidtree.hitax.agent.tools

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.ClassroomRoom
import com.stupidtree.hita.api.EmptyClassroomService
import com.google.gson.JsonObject
import com.stupidtree.hitax.HApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SearchEmptyClassroomTool : SuspendingReActTool {
    override suspend fun execute(input: ReActToolInput): String {
        val params = parseParams(input.arguments)
        val service = (input.application as HApplication).pluginHost
            .service(EmptyClassroomService.KEY)
            ?: return "空教室服务不可用（教务插件未提供）"
        val buildingsState = service.buildings().await(10_000)
            ?: return "教学楼查询超时"
        if (buildingsState.state != DataState.STATE.SUCCESS) {
            return "获取教学楼失败：${buildingsState.message ?: buildingsState.state}"
        }
        val all = buildingsState.data.orEmpty()
        val buildings = params.building?.takeIf { it.isNotBlank() }?.let { key ->
            all.filter { it.name.contains(key, true) || it.id.contains(key, true) }
        } ?: all
        if (buildings.isEmpty()) return "未找到教学楼「${params.building}」"
        val results = mutableListOf<Pair<String, List<ClassroomRoom>>>()
        val failures = mutableListOf<String>()
        for (building in buildings) {
            val state = service.query(building.id, params.week).await(18_000)
            if (state?.state == DataState.STATE.SUCCESS) {
                results += building.name to state.data.orEmpty()
            } else {
                failures += "${building.name}：${state?.message ?: "超时"}"
            }
        }
        if (results.isEmpty()) return "空教室查询失败：${failures.joinToString("；")}"
        return buildString {
            append("第 ${params.week} 周空教室")
            params.building?.let { append("（$it）") }
            append("：\n")
            results.forEach { (building, rooms) ->
                append("【$building】\n")
                rooms.take(30).forEach { room ->
                    val free = when {
                        params.day != null -> {
                            val occupied = room.occupied.filter { it.dayOfWeek == params.day }.map { it.period }.toSet()
                            (1..12).filter { it !in occupied }
                        }
                        else -> emptyList()
                    }
                    append("  ${room.name}")
                    if (params.day != null) {
                        append(if (free.isEmpty()) " 无空闲" else " 空闲: ${free.joinToString(",")}节")
                    }
                    append('\n')
                }
            }
        }.trimEnd()
    }

    internal data class Params(val week: Int, val building: String?, val day: Int?)

    internal fun parseParams(arguments: JsonObject): Params = Params(
        week = (arguments.intArg("week") ?: 1).coerceAtLeast(1),
        building = arguments.stringArg("building"),
        day = arguments.intArg("day")?.takeIf { it in 1..7 },
    )

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
