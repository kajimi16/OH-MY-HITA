package com.stupidtree.hita.ui.score

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.ScoreReport
import com.stupidtree.hita.api.ScoreService

class ScoreUiPlugin : ComposeUiPlugin {
    override val id = "com.stupidtree.hita.ui.score"
    override val displayName = "成绩分析"
    override val requiresApi = 7

    override fun createView(context: Context, host: PluginUiHost): View {
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    val service = host.service(ScoreService.KEY)
                    val termsLive = remember(service) {
                        service?.terms()
                            ?: androidx.lifecycle.MutableLiveData(DataState(emptyList()))
                    }
                    val termsState by termsLive.observeAsState(DataState(emptyList()))
                    val term = termsState.data?.firstOrNull { it.isCurrent }
                        ?: termsState.data?.firstOrNull()
                    val reportLive = remember(service, term) {
                        term?.let { service?.report(it) }
                            ?: androidx.lifecycle.MutableLiveData<DataState<ScoreReport>>(
                                DataState(DataState.STATE.NOTHING)
                            )
                    }
                    val reportState by reportLive.observeAsState(DataState(DataState.STATE.NOTHING))
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text("成绩分析", style = MaterialTheme.typography.h5)
                        when {
                            service == null -> Text("成绩服务不可用（教务插件缺失）")
                            termsState.state == DataState.STATE.NOT_LOGGED_IN ->
                                Text("未登录教务，请先在侧边栏登录")
                            term == null -> Text("暂无学期数据")
                            reportState.state == DataState.STATE.NOT_LOGGED_IN ->
                                Text("未登录教务，请先在侧边栏登录")
                            else -> {
                                val report = reportState.data
                                Text(term.name, style = MaterialTheme.typography.subtitle1)
                                if (report == null) {
                                    Text("加载中…")
                                } else {
                                    Text("GPA ${report.gpa.ifBlank { "-" }} · 排名 ${report.rank.ifBlank { "-" }}")
                                    Text("学分 ${report.total.ifBlank { "-" }}")
                                    LazyColumn(Modifier.padding(top = 8.dp)) {
                                        items(report.entries) { entry ->
                                            Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                                Column(Modifier.padding(10.dp)) {
                                                    Text(entry.courseName)
                                                    Text(
                                                        "${entry.credits} 学分 · ${entry.score} 分",
                                                        style = MaterialTheme.typography.caption,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
