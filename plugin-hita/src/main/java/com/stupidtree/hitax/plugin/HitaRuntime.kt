package com.stupidtree.hitax.plugin

import com.stupidtree.hita.api.HostUiService
import com.stupidtree.hita.api.TimetableService

/**
 * 插件内的运行时访问点。
 *
 * 插件自己的 Activity / ViewModel 拿不到 PluginContext，所以在 install 时把
 * 服务实例存这里，供插件内部的界面代码使用。
 */
object HitaRuntime {
    @Volatile var timetable: TimetableService? = null
    @Volatile var hostUi: HostUiService? = null

    fun timetableOrThrow(): TimetableService =
        timetable ?: error("timetable service not installed")
}
