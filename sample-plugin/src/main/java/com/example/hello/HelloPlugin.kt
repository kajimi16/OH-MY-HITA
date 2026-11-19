package com.example.hello

import android.util.Log
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.ServiceKey

/**
 * 最小逻辑插件示例：只注册一个服务，用来验证市场安装 / 卸载链路。
 *
 * 它没有界面，也不会改变任何现有功能——内置的夜间模式是宿主设置，
 * 与插件无关。需要界面的示例见 hita-plugin-template 的 ui-plugin 模块。
 */
class HelloPlugin : Plugin {

    override val id = "com.example.hello"

    override val displayName = "示例逻辑插件"

    override val requiresApi = 2

    override fun install(ctx: PluginContext) {
        ctx.provide(GREETING, "hello from runtime plugin")
        Log.i(TAG, "sample logic plugin installed")
    }

    override fun uninstall(ctx: PluginContext) {
        Log.i(TAG, "sample logic plugin uninstalled")
    }

    companion object {
        private const val TAG = "HelloPlugin"
        val GREETING = ServiceKey<String>("demo.greeting")
    }
}
