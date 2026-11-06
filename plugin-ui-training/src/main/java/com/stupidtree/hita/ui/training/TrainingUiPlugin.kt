package com.stupidtree.hita.ui.training

import android.content.Context
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.TrainingPlanService
import com.stupidtree.hitax.data.model.eas.ShenzhenTrainingPlan
import com.stupidtree.hitax.data.model.eas.ShenzhenTrainingPlanDetail

class TrainingUiPlugin : ComposeUiPlugin {
    override val id = "com.stupidtree.hita.ui.training"
    override val displayName = "培养方案"
    override val requiresApi = 7

    override fun createView(context: Context, host: PluginUiHost): View {
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    val service = host.service(TrainingPlanService.KEY)
                    val plansState by (service?.plans()
                        ?: androidx.lifecycle.MutableLiveData(DataState(DataState.STATE.NOTHING)))
                        .observeAsState(DataState(DataState.STATE.NOTHING))
                    var selected by remember { mutableStateOf<ShenzhenTrainingPlan?>(null) }
                    val detailState by (selected?.let { service?.detail(it) }
                        ?: androidx.lifecycle.MutableLiveData(DataState(DataState.STATE.NOTHING)))
                        .observeAsState(DataState(DataState.STATE.NOTHING))

                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text("培养方案", style = MaterialTheme.typography.h5)
                        when {
                            service == null -> Text("培养方案服务不可用")
                            plansState.state == DataState.STATE.NOT_LOGGED_IN ->
                                Text("请先在侧边栏连接深圳 Web 教务")
                            plansState.state != DataState.STATE.SUCCESS ->
                                Text("加载失败：${plansState.message ?: plansState.state}")
                            plansState.data.isNullOrEmpty() -> Text("暂无培养方案")
                            else -> {
                                if (selected == null) {
                                    LazyColumn(Modifier.padding(top = 8.dp)) {
                                        items(plansState.data.orEmpty()) { plan ->
                                            Card(
                                                Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                                    .clickable { selected = plan },
                                            ) {
                                                Column(Modifier.padding(12.dp)) {
                                                    Text(plan.name, style = MaterialTheme.typography.subtitle1)
                                                    Text("${plan.majorName} · ${plan.grade}",
                                                        style = MaterialTheme.typography.caption)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val plan = selected!!
                                    Text(plan.name, style = MaterialTheme.typography.subtitle1)
                                    Text("返回列表", modifier = Modifier.clickable { selected = null },
                                        style = MaterialTheme.typography.caption)
                                    val detail = detailState.data
                                    if (detail == null) {
                                        Text("加载中…")
                                    } else {
                                        PlanDetail(detail)
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

@androidx.compose.runtime.Composable
private fun PlanDetail(detail: ShenzhenTrainingPlanDetail) {
    val completed = detail.courses.filter { it.completed == true }
        .mapNotNull { it.credits }.sum()
    val total = detail.totalCredits
    val percent = if (total > 0) (completed / total * 100).toInt() else 0
    Text("总学分 ${"%.1f".format(total)} · 已完成 ${"%.1f".format(completed)} · $percent%",
        style = MaterialTheme.typography.body2)
    LazyColumn(Modifier.padding(top = 8.dp)) {
        items(detail.categories) { category ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(10.dp)) {
                    Text(category.name, style = MaterialTheme.typography.subtitle1)
                    Text("${category.courses.size} 门 · ${"%.1f".format(category.credits)} 学分",
                        style = MaterialTheme.typography.caption)
                    category.courses.take(8).forEach { course ->
                        Text(
                            "${if (course.completed == true) "✓ " else ""}${course.courseName} " +
                                "${course.credits ?: 0.0} 学分",
                            style = MaterialTheme.typography.caption,
                        )
                    }
                }
            }
        }
    }
}
