package com.stupidtree.hitax.plugin

import com.stupidtree.hita.api.CommunityService
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.TimetableService

/** 插件内界面代码访问其它插件服务的运行时入口（延迟解析，避免安装顺序影响）。 */
object UserRuntime {
    @Volatile
    var ctx: PluginContext? = null

    val community: CommunityService? get() = ctx?.service(CommunityService.KEY)
    val timetable: TimetableService? get() = ctx?.service(TimetableService.KEY)
}
