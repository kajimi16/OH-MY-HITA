package com.example.hitaui.sample

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.TimetableService

/**
 * 最小可用的运行时界面插件：验证宿主容器、契约注入与生命周期。
 */
class SampleUiPlugin : ComposeUiPlugin {

    override val id = "com.example.hitaui.sample"

    override val displayName = "示例界面插件"

    override val requiresApi = 3

    override fun createView(context: Context, host: PluginUiHost): View {
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    val service = remember { host.service(TimetableService.KEY) }
                    var today by remember { mutableStateOf<Int?>(null) }
                    var failure by remember { mutableStateOf<String?>(null) }
                    LaunchedEffect(service) {
                        if (service != null) {
                            runCatching {
                                withContext(Dispatchers.IO) { service.getTodayEventsSync().size }
                            }.onSuccess { today = it }
                                .onFailure { failure = it.message ?: it::class.java.simpleName }
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("运行时 Compose 插件", style = MaterialTheme.typography.h5)
                        Text("插件版本：${BuildConfig.VERSION_NAME}")
                        Text("宿主提供的能力：TimetableService")
                        Text(
                            when {
                                service == null -> "课表插件不可用"
                                failure != null -> "读取失败：$failure"
                                today != null -> "今日事件：$today 项"
                                else -> "正在读取课表…"
                            }
                        )
                        Button(onClick = { host.close() }) {
                            Text("关闭")
                        }
                    }
                }
            }
        }
    }
}
