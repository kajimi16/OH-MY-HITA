package com.stupidtree.hitax.plugin

import android.app.Application
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.UserUiService

/** 个人主页插件：账号资料页与编辑页。 */
class UserPlugin(private val application: Application) : Plugin {

    override val id = "com.stupidtree.hitax.user"

    override val displayName = "个人主页"

    override fun install(ctx: PluginContext) {
        UserRuntime.ctx = ctx
        ctx.provide(UserUiService.KEY, UserUiServiceImpl(application))
    }
}
