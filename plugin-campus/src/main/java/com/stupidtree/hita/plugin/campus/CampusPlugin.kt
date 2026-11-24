package com.stupidtree.hita.plugin.campus

import com.stupidtree.hita.api.CampusService
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext

/**
 * 校园扩展插件入口。
 *
 * 只注册 [CampusService]；登录仍由宿主 WebView 完成，本插件不注册 Activity、
 * 不实现登录界面，也不持久化任何凭据。
 */
class CampusPlugin : Plugin {
    override val id = "com.stupidtree.hita.plugin.campus"
    override val displayName = "校园扩展"
    override val requiresApi = 12

    @Volatile private var service: CampusServiceImpl? = null

    override fun install(ctx: PluginContext) {
        val cookies = CampusCookieStore()
        val impl = CampusServiceImpl(cookies)
        service = impl
        ctx.provide(CampusService.KEY, impl)
    }

    override fun uninstall(ctx: PluginContext) {
        service = null
    }
}
