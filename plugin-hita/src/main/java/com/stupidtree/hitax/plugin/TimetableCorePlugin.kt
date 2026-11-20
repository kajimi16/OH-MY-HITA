package com.stupidtree.hitax.plugin

import android.app.Application
import com.stupidtree.hita.api.HostEnvironmentService
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext

/**
 * 运行时 dex 的无参入口。
 *
 * 内核按 `getDeclaredConstructor()` 实例化入口类，而 [HitaPlugin] 需要 Application。
 * 该包装从宿主环境取 applicationContext，再把注册动作交给 [HitaPlugin]。
 */
class TimetableCorePlugin : Plugin {

    private var delegate: HitaPlugin? = null

    override val id = "com.stupidtree.hitax.timetable"

    override val requiresApi = 11

    override val displayName = "课表"

    override fun install(ctx: PluginContext) {
        val application = ctx.service(HostEnvironmentService.KEY)!!.applicationContext as Application
        delegate = HitaPlugin(application).also { it.install(ctx) }
    }

    override fun uninstall(ctx: PluginContext) {
        delegate?.uninstall(ctx)
    }

    override fun purgeData() {
        delegate?.purgeData()
    }
}
