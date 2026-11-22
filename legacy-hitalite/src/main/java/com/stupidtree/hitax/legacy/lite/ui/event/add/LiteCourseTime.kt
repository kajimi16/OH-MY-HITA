package com.stupidtree.hitax.legacy.lite.ui.event.add

import com.stupidtree.hitax.data.model.timetable.TimeInDay
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay

/** 添加事件弹窗内部使用的时间段草稿。 */
class LiteCourseTime {
    var period: TimePeriodInDay = TimePeriodInDay(TimeInDay(0, 0), TimeInDay(0, 0))
    var dow: Int = 1
    var weeks: List<Int> = mutableListOf()
}
