package com.stupidtree.hitax.agent.tools

import com.stupidtree.hitax.HitaServices
import com.stupidtree.hitax.agent.model.AgentResourceCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ToolHelper {
    fun parseDayOffset(text: String): Int {
        val cal = Calendar.getInstance()
        val todayDow = cal.get(Calendar.DAY_OF_WEEK)
        val dowMap = mapOf(
            "周一" to Calendar.MONDAY, "星期一" to Calendar.MONDAY,
            "周二" to Calendar.TUESDAY, "星期二" to Calendar.TUESDAY,
            "周三" to Calendar.WEDNESDAY, "星期三" to Calendar.WEDNESDAY,
            "周四" to Calendar.THURSDAY, "星期四" to Calendar.THURSDAY,
            "周五" to Calendar.FRIDAY, "星期五" to Calendar.FRIDAY,
            "周六" to Calendar.SATURDAY, "星期六" to Calendar.SATURDAY,
            "周日" to Calendar.SUNDAY, "星期日" to Calendar.SUNDAY, "周天" to Calendar.SUNDAY,
        )
        if (text.contains("今天")) return 0
        if (text.contains("大前天")) return -3
        if (text.contains("前天")) return -2
        if (text.contains("昨天")) return -1
        if (text.contains("明天")) return 1
        if (text.contains("大后天")) return 3
        if (text.contains("后天")) return 2
        for ((label, dow) in dowMap) {
            if (text.contains(label)) {
                var diff = dow - todayDow
                diff = when {
                    text.contains("上个") || text.contains("上周") ->
                        if (diff >= 0) diff - 7 else diff
                    text.contains("下个") || text.contains("下周") ->
                        if (diff <= 0) diff + 7 else diff
                    else -> diff
                }
                return diff
            }
        }
        val match = Regex("(\\d{1,2})月(\\d{1,2})").find(text) ?: return 0
        val month = match.groupValues[1].toIntOrNull() ?: return 0
        val day = match.groupValues[2].toIntOrNull() ?: return 0
        val target = Calendar.getInstance().apply {
            set(Calendar.MONTH, month - 1); set(Calendar.DAY_OF_MONTH, day)
        }
        return ((target.timeInMillis - cal.timeInMillis) / (24 * 3600 * 1000)).toInt()
    }

    fun timetableText(input: ReActToolInput): String {
        val service = HitaServices.timetable(input.application) ?: return "课表服务不可用"
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, parseDayOffset(input.userMessage))
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val from = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val events = service.getEventsDuringSync(from, cal.timeInMillis)
        val label = SimpleDateFormat("yyyy-MM-dd EEEE", Locale.CHINA).format(Date(from))
        if (events.isEmpty()) return "[$label] 没有课程安排"
        val fmt = SimpleDateFormat("HH:mm", Locale.CHINA)
        return buildString {
            append("[$label]\n")
            events.take(15).forEach { event ->
                append("- ${event.name}  ${fmt.format(event.from)}~${fmt.format(event.to)}")
                event.place?.takeIf { it.isNotBlank() }?.let { append("  @ $it") }
                append('\n')
            }
        }.trimEnd()
    }
}
