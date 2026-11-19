package com.stupidtree.hitax.data.model.timetable

import java.util.Calendar

/** 一周的毫秒数。原先定义在 UI 的 `TimetableFragment.Companion` 里，实体反向依赖了 UI。 */
const val WEEK_MILLS: Long = 1000L * 60 * 60 * 24 * 7

/**
 * 纯时间计算，供实体与 DAO 使用。
 *
 * 与 `utils/TimeTools` 的区别：这里不碰 Android 资源（`R`/`Context`），
 * 所以数据层可以在没有 UI 的模块里单独编译与测试。
 */
object TimeMath {

    /** 某时间所在周的周一 0 点。 */
    fun getMonday(ts: Long): Calendar {
        val res = Calendar.getInstance()
        res.timeInMillis = ts
        res.firstDayOfWeek = Calendar.MONDAY
        res[Calendar.DAY_OF_WEEK] = Calendar.MONDAY
        res[Calendar.HOUR_OF_DAY] = 0
        res[Calendar.MINUTE] = 0
        return res
    }

    /** 周几，周一为 1。 */
    fun getDow(ts: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = ts
        c.firstDayOfWeek = Calendar.MONDAY
        return if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            7
        } else {
            c.get(Calendar.DAY_OF_WEEK) - 1
        }
    }

    /** 小时（东八区）。 */
    fun getHour(date: Long): Int {
        return ((8 + ((date % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))) % 24).toInt()
    }

    fun getMinute(date: Long): Int {
        return ((date % (1000 * 60 * 60)) / (1000 * 60)).toInt()
    }
}
