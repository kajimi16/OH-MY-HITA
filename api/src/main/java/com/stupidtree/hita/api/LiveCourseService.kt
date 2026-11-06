package com.stupidtree.hita.api

/** 一场直播课。 */
data class LiveCourseSpec(
    val ownerId: String,
    val id: String,
    val title: String,
    val url: String,
    val startAtMillis: Long,
    val endAtMillis: Long,
    val advanceMinutes: Int = 5,
)

/**
 * 宿主直播课能力（API 7）：AlarmManager 触发前台服务 + 通知，开机恢复。
 * 插件没有 manifest，不能自己声明 Service/Receiver。
 */
interface LiveCourseService {
    fun schedule(spec: LiveCourseSpec): Boolean
    fun cancel(ownerId: String, id: String)
    fun cancelAll(ownerId: String)

    companion object {
        val KEY = ServiceKey<LiveCourseService>("hita.liveCourse")
    }
}
