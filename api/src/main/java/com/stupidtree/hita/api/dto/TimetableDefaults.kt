package com.stupidtree.hita.api.dto

/**
 * 课表默认课时结构。
 *
 * 必须与 `com.stupidtree.hitax.data.model.timetable.Timetable.getDefaultTimeStructure()`
 * 的 12 节完全一致；`EntityMappersTest` 会逐项比对。
 */
object TimetableDefaults {
    fun periods(): List<TimePeriodDto> = listOf(
        TimePeriodDto(8, 30, 9, 20),
        TimePeriodDto(9, 25, 10, 15),
        TimePeriodDto(10, 30, 11, 20),
        TimePeriodDto(11, 25, 12, 15),
        TimePeriodDto(14, 0, 14, 50),
        TimePeriodDto(14, 55, 15, 45),
        TimePeriodDto(16, 0, 16, 50),
        TimePeriodDto(16, 55, 17, 45),
        TimePeriodDto(18, 45, 19, 35),
        TimePeriodDto(19, 40, 20, 30),
        TimePeriodDto(20, 45, 21, 35),
        TimePeriodDto(21, 40, 22, 30),
    )
}
