package com.stupidtree.hita.plugin.campusui

import android.content.Context
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.ComposeView
import com.stupidtree.hita.api.CampusService
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost

/**
 * 校园运行时界面插件入口。
 *
 * 前置条件：宿主已完成 Compose 运行时与插件依赖校验；本类只被 DexClassLoader 实例化。
 * 后置条件：返回填好内容的 ComposeView，由宿主容器承载。
 * 不变式：id 与 assets/plugin.json 一致；只经 [PluginUiHost] 取服务，不访问宿主 Activity。
 * 违约处理：CampusService 缺失时显示不可用提示，仍允许关闭。
 */
class CampusUiPlugin : ComposeUiPlugin {

    override val id = "com.stupidtree.hita.ui.campus"

    override val displayName = "校园"

    override val requiresApi = 11

    override fun createView(context: Context, host: PluginUiHost): View {
        return ComposeView(context).apply {
            setContent {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = Color(0xFF007AFF),
                        background = Color.White,
                        surface = Color.White,
                        onPrimary = Color.White,
                        onBackground = Color(0xFF1C1C1E),
                        onSurface = Color(0xFF1C1C1E),
                        onSurfaceVariant = Color(0xFF6B6B70),
                        surfaceVariant = Color(0xFFF2F2F7),
                    ),
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val service = remember(host) { host.service(CampusService.KEY) }
                        if (service == null) {
                            ServiceUnavailable(host)
                        } else {
                            CampusRoot(host, service)
                        }
                    }
                }
            }
        }
    }
}
