package com.stupidtree.hitax.agent.tools

import com.google.gson.JsonObject
import com.stupidtree.hitax.HitaServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 在宿主课表内按课程名/教师/地点检索，不依赖上游 TimetableAgentInput。 */
class SearchTimetableTool : ReActTool {
    override fun execute(input: ReActToolInput): String {
        val keyword = extractKeyword(input.arguments)
            ?: input.actionInput.trim().removeSurrounding("{", "}")
        if (keyword.isBlank()) return "搜索失败: 无法解析关键词"
        val service = HitaServices.timetable(input.application) ?: return "课表服务不可用"
        val now = System.currentTimeMillis()
        val from = now - 120L * 24 * 60 * 60 * 1000
        val to = now + 120L * 24 * 60 * 60 * 1000
        val events = service.getEventsDuringSync(from, to)
            .filter { input.timetableId == null || it.timetableId == input.timetableId }
            .filter {
                it.name.contains(keyword, ignoreCase = true) ||
                    it.teacher?.contains(keyword, ignoreCase = true) == true ||
                    it.place?.contains(keyword, ignoreCase = true) == true
            }
            .sortedBy { it.from.time }
        if (events.isEmpty()) return "未找到与“$keyword”相关的课程"
        val date = SimpleDateFormat("MM-dd EEE HH:mm", Locale.CHINA)
        return buildString {
            append("找到 ${events.size} 个与“$keyword”相关的课程安排：\n")
            events.take(20).forEach { event ->
                append("- ${event.name}  ")
                append(date.format(Date(event.from.time)))
                append("~")
                append(SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(event.to.time)))
                event.place?.takeIf { it.isNotBlank() }?.let { append("  @ $it") }
                event.teacher?.takeIf { it.isNotBlank() }?.let { append("  教师: $it") }
                append('\n')
            }
        }.trimEnd()
    }

    internal fun extractKeyword(arguments: JsonObject): String? =
        arguments.stringArg("query", "keyword", "param", "name")
}
