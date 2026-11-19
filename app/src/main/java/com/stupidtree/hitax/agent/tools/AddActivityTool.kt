package com.stupidtree.hitax.agent.tools

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.stupidtree.hitax.HitaServices
import com.stupidtree.hitax.data.model.timetable.EventItem
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddActivityTool : SuspendingReActTool {
    override suspend fun execute(input: ReActToolInput): String? {
        val parsed = parseAddActivityInput(input.arguments)
        val name = parsed.name ?: return "未找到活动信息"
        val fromMs = parsed.fromMs ?: return "未找到开始时间"
        val toMs = parsed.toMs ?: return "未找到结束时间"
        val now = System.currentTimeMillis()
        val oneYear = 365L * 24 * 60 * 60 * 1000
        if (fromMs < now - oneYear || fromMs > now + oneYear ||
            toMs < now - oneYear || toMs > now + oneYear
        ) {
            return "时间戳不合理，请重新描述时间（如'明天下午3点'）"
        }
        val service = HitaServices.timetable(input.application) ?: return "课表服务不可用"
        val timetableId = input.timetableId
            ?: service.getRecentTimetable().value?.id
            ?: return "当前没有可写入的课表"
        val event = EventItem().apply {
            this.name = name
            this.place = parsed.place
            this.type = EventItem.TYPE.OTHER
            this.timetableId = timetableId
            this.from = Timestamp(fromMs)
            this.to = Timestamp(toMs)
        }
        service.actionAddEvents(listOf(event))
        val label = SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(fromMs)
        return "已添加活动：$name，$label"
    }

    internal data class ParsedActivity(val name: String?, val fromMs: Long?, val toMs: Long?, val place: String)

    internal fun parseAddActivityInput(arguments: JsonObject): ParsedActivity {
        val name = arguments.stringArg("name")
        val place = arguments.stringArg("place") ?: ""
        val dayOffset = arguments.intArg("day_offset")
        val start = arguments.stringArg("start_time")
        val end = arguments.stringArg("end_time")
        if (name != null && dayOffset != null && start != null && end != null) {
            val from = parseRelativeTime(dayOffset, start)
            val to = parseRelativeTime(dayOffset, end)
            if (from != null && to != null) return ParsedActivity(name, from, to, place)
        }
        val from = parseTimeValue(arguments.get("from"))
        val to = parseTimeValue(arguments.get("to"))
        if (name != null && from != null && to != null) return ParsedActivity(name, from, to, place)
        return ParsedActivity(null, null, null, "")
    }

    private fun parseTimeValue(value: JsonElement?): Long? = when {
        value == null || value.isJsonNull || !value.isJsonPrimitive -> null
        value.asJsonPrimitive.isNumber -> value.asLong
        value.asJsonPrimitive.isString -> parseTimestampOrIso(value.asString)
        else -> null
    }

    private fun parseTimestampOrIso(raw: String): Long? {
        val trimmed = raw.trim().removeSurrounding("\"")
        trimmed.toLongOrNull()?.let { return it }
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd HH:mm",
        )
        for (pattern in formats) {
            runCatching { SimpleDateFormat(pattern, Locale.CHINA).parse(trimmed)?.time }
                .getOrNull()?.let { return it }
        }
        return null
    }

    private fun parseRelativeTime(dayOffset: Int, time: String): Long? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
