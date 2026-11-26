package com.stupidtree.hitax.eas

import com.stupidtree.hita.api.HostUiService
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.TimetableProviderService
import com.stupidtree.hita.api.TimetableService

/**
 * 插件内运行时服务访问点。
 *
 * 只保存 PluginContext，服务每次现查：课表插件被卸载/重装后不会拿到旧实例。
 */
object EasRuntime {
    @Volatile
    var ctx: PluginContext? = null

    val timetable: TimetableService? get() = ctx?.service(TimetableService.KEY)
    /** 旧版课表核心没有该能力；取不到时退回 [timetable] 的按 code 查询。 */
    val timetableProvider: TimetableProviderService? get() = ctx?.service(TimetableProviderService.KEY)
    val hostUi: HostUiService? get() = ctx?.service(HostUiService.KEY)

    fun timetableOrThrow(): TimetableService =
        timetable ?: error("timetable service not installed")
}
