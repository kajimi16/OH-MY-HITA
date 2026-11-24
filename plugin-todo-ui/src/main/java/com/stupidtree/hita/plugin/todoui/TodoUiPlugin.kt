package com.stupidtree.hita.plugin.todoui

import android.content.Context
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.TodoService

/**
 * 待办运行时界面插件入口。
 *
 * 契约：id 与 assets/plugin.json 的 id 一致；只消费 [TodoService]，不访问宿主 Activity。
 * 服务缺失时界面显示不可用提示，仍可关闭。
 */
class TodoUiPlugin : ComposeUiPlugin {

    override val id = "com.stupidtree.hita.ui.todo"

    override val displayName = "待办"

    override val requiresApi = 11

    override fun createView(context: Context, host: PluginUiHost): View {
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    val service = remember(host) { host.service(TodoService.KEY) }
                    if (service == null) {
                        TodoUnavailable(host)
                    } else {
                        TodoScreen(host, service)
                    }
                }
            }
        }
    }
}
