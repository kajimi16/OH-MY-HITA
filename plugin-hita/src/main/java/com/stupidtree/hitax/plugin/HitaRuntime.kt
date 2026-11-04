package com.stupidtree.hitax.plugin

import com.stupidtree.hita.api.HostUiService
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hitax.data.model.timetable.EventItem

/**
 * 插件内的运行时访问点。
 *
 * 插件自己的 Activity / ViewModel 拿不到 PluginContext，所以在 install 时把
 * 服务实例存这里，供插件内部的界面代码使用。
 */
object HitaRuntime {
    @Volatile var timetable: TimetableService? = null
    @Volatile var hostUi: HostUiService? = null

    /** 小组件用：插件缺席（空壳）时给空列表，避免 RemoteViews 崩溃。 */
    fun todayEvents(): List<EventItem> = timetable?.getTodayEventsSync() ?: emptyList()

    fun timetableOrThrow(): TimetableService =
        timetable ?: error("timetable service not installed")
}
