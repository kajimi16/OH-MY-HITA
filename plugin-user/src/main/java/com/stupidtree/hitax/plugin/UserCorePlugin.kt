package com.stupidtree.hitax.plugin

import android.app.Application
import com.stupidtree.hita.api.HostEnvironmentService
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext

/** 运行时 dex 的无参入口，委托给需要 Application 的 [UserPlugin]。 */
class UserCorePlugin : Plugin {

    private var delegate: UserPlugin? = null

    override val id = "com.stupidtree.hitax.user"

    override val requiresApi = 11

    override val displayName = "个人主页"

    override fun install(ctx: PluginContext) {
        val application = ctx.service(HostEnvironmentService.KEY)!!.applicationContext as Application
        delegate = UserPlugin(application).also { it.install(ctx) }
    }

    override fun uninstall(ctx: PluginContext) {
        delegate?.uninstall(ctx)
    }

    override fun purgeData() {
        delegate?.purgeData()
    }
}
