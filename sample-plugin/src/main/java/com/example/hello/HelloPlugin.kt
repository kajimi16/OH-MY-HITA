package com.example.hello

import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.ServiceKey

/**
 * T1 示例插件：只提供逻辑，没有界面与资源。
 *
 * 编译时依赖 `:api`（compileOnly），运行时的类由宿主提供。
 */
class HelloPlugin : Plugin {

    override val id = "com.example.hello"

    override val displayName = "示例插件"

    override val requiresApi = 2

    override fun install(ctx: PluginContext) {
        ctx.provide(GREETING, "hello from runtime plugin")
    }

    companion object {
        val GREETING = ServiceKey<String>("demo.greeting")
    }
}
