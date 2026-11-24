package com.stupidtree.hita.plugin.todo

import android.app.Application
import com.stupidtree.hita.api.HostEnvironmentService
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.TodoService

/**
 * 待办插件入口。
 *
 * 只注册 [TodoService]，不注册任何 Activity 或 Compose 页面；
 * Grader 的 WebView/CAS 登录流程留给界面层，本插件只接收会话 cookie。
 */
class TodoPlugin : Plugin {
    override val id = "com.stupidtree.hita.plugin.todo"
    override val displayName = "待办聚合"
    override val requiresApi = 12

    @Volatile private var service: TodoServiceImpl? = null

    override fun install(ctx: PluginContext) {
        val application = (ctx.service(HostEnvironmentService.KEY)?.applicationContext as? Application)
            ?: error("缺少宿主 applicationContext")
        val impl = TodoServiceImpl(application)
        service = impl
        ctx.provide(TodoService.KEY, impl)
    }

    override fun uninstall(ctx: PluginContext) {
        service?.close()
        service = null
    }

    override fun purgeData() {
        service?.purge()
    }
}
