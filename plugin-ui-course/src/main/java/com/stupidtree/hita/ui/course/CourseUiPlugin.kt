package com.stupidtree.hita.ui.course

import android.content.Context
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.CourseCatalogService
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.ScoreService
import com.stupidtree.hitax.data.model.eas.ShenzhenCourseCatalogPage
import com.stupidtree.hitax.data.model.eas.ShenzhenSelectionPools

class CourseUiPlugin : ComposeUiPlugin {
    override val id = "com.stupidtree.hita.ui.course"
    override val displayName = "选课助手"
    override val requiresApi = 7

    override fun createView(context: Context, host: PluginUiHost): View {
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    val catalog = host.service(CourseCatalogService.KEY)
                    val scores = host.service(ScoreService.KEY)
                    val termsState by (scores?.terms()
                        ?: androidx.lifecycle.MutableLiveData(DataState(DataState.STATE.NOTHING)))
                        .observeAsState(DataState(DataState.STATE.NOTHING))
                    val term = termsState.data?.firstOrNull { it.isCurrent }
                        ?: termsState.data?.firstOrNull()
                    var pool by remember { mutableStateOf(ShenzhenSelectionPools.all.first()) }
                    val pageLive = remember(term?.getCode(), pool.code, catalog) {
                        if (term != null && catalog != null) catalog.available(term, pool.code)
                        else androidx.lifecycle.MutableLiveData(DataState(DataState.STATE.NOTHING))
                    }
                    val page by pageLive.observeAsState(DataState(DataState.STATE.NOTHING))
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text("选课助手", style = MaterialTheme.typography.h5)
                        Text("只读课程目录；暂不执行选课。", style = MaterialTheme.typography.caption)
                        when {
                            catalog == null -> Text("选课服务不可用")
                            termsState.state == DataState.STATE.NOT_LOGGED_IN ->
                                Text("请先连接深圳 Web 教务")
                            term == null -> Text("暂无学期")
                            else -> {
                                Text("课程池", style = MaterialTheme.typography.subtitle1)
                                LazyColumn(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    items(ShenzhenSelectionPools.all) { p ->
                                        Text(
                                            (if (p.code == pool.code) "✓ " else "") + p.name,
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                pool = p
                                            }.padding(6.dp),
                                        )
                                    }
                                }
                                Text("结果", style = MaterialTheme.typography.subtitle1)
                                when {
                                    page.state != DataState.STATE.SUCCESS -> Text("加载中/未查询")
                                    page.data?.items.isNullOrEmpty() -> Text("没有课程")
                                    else -> LazyColumn {
                                        items(page.data!!.items) { item ->
                                            Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                                Column(Modifier.padding(10.dp)) {
                                                    Text("${item.courseName}（${item.courseCode}）")
                                                    Text(
                                                        "${item.teacher} · ${item.credits} 学分 · ${item.schedule}",
                                                        style = MaterialTheme.typography.caption,
                                                    )
                                                    Text(
                                                        "余量 ${item.remainingSeats ?: "-"}",
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
