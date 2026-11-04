package com.stupidtree.hitax.eas

import android.app.Application
import com.stupidtree.hita.api.EasService
import com.stupidtree.hita.api.HostUiService
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.TimetableService

/** 教务插件：提供 [EasService]，并把依赖的宿主能力放进 [EasRuntime]。 */
class EasPlugin(private val application: Application) : Plugin {

    override val id = "com.stupidtree.hitax.eas"

    override val displayName = "教务"

    override fun install(ctx: PluginContext) {
        EasRuntime.timetable = ctx.service(TimetableService.KEY)
        EasRuntime.hostUi = ctx.service(HostUiService.KEY)
        ctx.provide(EasService.KEY, EasServiceImpl(application))
    }
}
