package com.example.hello

import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.ThemeController

/**
 * T1 示例插件：安装后把应用切到夜间模式，卸载时恢复。
 *
 * 它只能通过宿主提供的 [ThemeController] 契约完成这件事——
 * 运行时插件没有 Context，也不允许自己碰 Android 主题 API。
 */
class HelloPlugin : Plugin {

    override val id = "com.example.hello"

    override val displayName = "夜间模式"

    override val requiresApi = 2

    override fun install(ctx: PluginContext) {
        ctx.service(ThemeController.KEY)?.setNightMode(true)
    }

    override fun uninstall(ctx: PluginContext) {
        ctx.service(ThemeController.KEY)?.setNightMode(false)
    }
}
