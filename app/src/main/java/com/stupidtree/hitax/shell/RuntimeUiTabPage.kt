package com.stupidtree.hitax.shell

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.MainTabSpec
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.ServiceKey
import com.stupidtree.hita.api.RuntimeUiService
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.uiplugin.UiPluginManager

private data class RuntimeTabHolder(
    val plugin: ComposeUiPlugin,
    val view: View,
)

/**
 * 在 Shell 内嵌已安装 UI 插件的主 tab。
 *
 * 插件仍由宿主容器承载，不注册 Activity；tab 参数通过 PluginUiHost.arguments 传入。
 */
@Composable
fun RuntimeUiTabPage(pluginId: String, spec: MainTabSpec, title: String) {
    val context = LocalContext.current
    val holder = remember(pluginId, spec.id) {
        runCatching {
            val manager = UiPluginManager(context)
            val record = manager.installed().firstOrNull { it.id == pluginId }
                ?: return@runCatching null
            val plugin = manager.instantiate(record)
            val arguments = Bundle().apply {
                putString("tabId", spec.id)
                putString("capability", spec.capability)
                putString("tabTitle", title)
            }
            val host = object : PluginUiHost {
                override fun <T : Any> service(key: ServiceKey<T>): T? =
                    (context.applicationContext as? HApplication)?.pluginHost?.service(key)

                override val arguments: Bundle get() = arguments
                override fun close() = Unit
            }
            RuntimeTabHolder(plugin, plugin.createView(context, host))
        }.getOrNull()
    }

    DisposableEffect(holder) {
        onDispose { runCatching { holder?.plugin?.onDestroy() } }
    }

    if (holder == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "$title 页面不可用",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        AndroidView(
            factory = { holder.view },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
