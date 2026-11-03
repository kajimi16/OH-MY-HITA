package com.stupidtree.hitax.data.model.timetable

/** ICS 导入的中间产物：一张课表 + 课程 + 事件。 */
data class IcsImportBundle(
    val timetable: Timetable,
    val subjects: List<TermSubject>,
    val events: List<EventItem>
)

/** 一次 ICS 导入的结果摘要，供 UI 提示。 */
data class IcsImportResult(
    val timetableId: String,
    val timetableName: String,
    val importedCount: Int
)
