package com.stupidtree.hita.ui.course

import java.util.Calendar

internal object SelectionAlarm {
    /** "HH:mm" -> 下一次触发时间（今天已过则明天）；非法返回 null。 */
    fun nextTrigger(hhmm: String, nowMillis: Long): Long? {
        val parts = hhmm.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= nowMillis) calendar.add(Calendar.DAY_OF_YEAR, 1)
        return calendar.timeInMillis
    }
}
