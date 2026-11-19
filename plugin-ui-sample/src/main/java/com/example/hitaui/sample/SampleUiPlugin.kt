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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.ResourceDataService
import com.stupidtree.component.data.DataState
import com.stupidtree.hitax.data.model.resource.CourseResourceItem
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
                    val resourceService = remember { host.service(ResourceDataService.KEY) }
                    val query = host.arguments.getString("query") ?: "CS"
                    val submitMode = host.arguments.getBoolean("submit", false)
                    val resourceQuery = remember(resourceService, query) {
                        resourceService?.searchCourses(query)
                    }
                    val emptyResource = remember {
                        androidx.lifecycle.MutableLiveData<DataState<List<CourseResourceItem>>>()
                    }
                    val resourceState by (resourceQuery ?: emptyResource).observeAsState()
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
                        Text("资源服务：${if (resourceService == null) "不可用" else "已连接"}",
                            style = MaterialTheme.typography.body2)
                        Text("课程资源搜索：$query", style = MaterialTheme.typography.subtitle1)
                        Text("提交模式：${if (submitMode) "是" else "否"}",
                            style = MaterialTheme.typography.body2)
                        Text(
                            when {
                                resourceService == null -> "ResourceDataService 未注册"
                                resourceState == null -> "课程资源查询中…"
                                resourceState?.state == DataState.STATE.SUCCESS ->
                                    "课程资源：${resourceState?.data?.size ?: 0} 条"
                                else -> "课程资源：${resourceState?.message ?: resourceState?.state}"
                            },
                            style = MaterialTheme.typography.body2,
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
