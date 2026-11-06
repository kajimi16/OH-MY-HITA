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
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.OutlinedTextField
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
import com.stupidtree.hita.api.ReminderService
import com.stupidtree.hita.api.ReminderSpec
import com.stupidtree.hita.api.ScoreService
import com.stupidtree.hitax.data.model.eas.ShenzhenCourseCatalogItem
import com.stupidtree.hitax.data.model.eas.ShenzhenCourseCatalogPage
import com.stupidtree.hitax.data.model.eas.ShenzhenSelectionPools
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CourseUiPlugin : ComposeUiPlugin {
    override val id = "com.stupidtree.hita.ui.course"
    override val displayName = "选课助手"
    override val requiresApi = 7

    override fun createView(context: Context, host: PluginUiHost): View {
        val prefs = context.getSharedPreferences("course_plan", Context.MODE_PRIVATE)
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
                    var selected by remember { mutableStateOf(loadPlan(prefs)) }
                    var remindAt by remember { mutableStateOf("08:00") }
                    var reminderStatus by remember { mutableStateOf("") }
                    val reminders = host.service(ReminderService.KEY)
                    val pageLive = remember(term?.getCode(), pool.code, catalog) {
                        if (term != null && catalog != null) catalog.available(term, pool.code)
                        else androidx.lifecycle.MutableLiveData(DataState(DataState.STATE.NOTHING))
                    }
                    val page by pageLive.observeAsState(DataState(DataState.STATE.NOTHING))
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text("选课助手", style = MaterialTheme.typography.h5)
                        Text("只读课程目录；可加入本地计划，暂不执行选课。", style = MaterialTheme.typography.caption)
                        Row {
                            OutlinedTextField(
                                value = remindAt,
                                onValueChange = { remindAt = it },
                                label = { Text("选课提醒 HH:mm") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            Button(onClick = {
                                val trigger = SelectionAlarm.nextTrigger(remindAt, System.currentTimeMillis())
                                if (trigger == null) {
                                    reminderStatus = "时间格式无效"
                                } else {
                                    val ok = reminders?.schedule(
                                        ReminderSpec(
                                            ownerId = "com.stupidtree.hita.ui.course",
                                            id = "selection-start",
                                            triggerAtMillis = trigger,
                                            title = "选课即将开始",
                                            text = "打开选课助手查看课程计划",
                                        )
                                    ) == true
                                    reminderStatus = if (ok) "已安排选课提醒" else "提醒失败（检查通知权限）"
                                }
                            }) { Text("提醒") }
                        }
                        if (reminderStatus.isNotBlank()) Text(reminderStatus, style = MaterialTheme.typography.caption)
                        if (selected.isNotEmpty()) {
                            val conflicts = CoursePlan.conflicts(selected)
                            Text(
                                "计划 ${selected.size} 门 · ${"%.1f".format(CoursePlan.credits(selected))} 学分 · 冲突 ${conflicts.size}",
                                style = MaterialTheme.typography.body2,
                            )
                            conflicts.forEach {
                                Text("冲突：${it.first.courseName} × ${it.second.courseName}",
                                    style = MaterialTheme.typography.caption)
                            }
                        }
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
                                                    val chosen = selected.any { it.id == item.id }
                                                    Button(onClick = {
                                                        selected = if (chosen) selected.filterNot { it.id == item.id }
                                                        else selected + item
                                                        savePlan(prefs, selected)
                                                    }) { Text(if (chosen) "移出计划" else "加入计划") }
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

    private fun loadPlan(prefs: android.content.SharedPreferences): List<ShenzhenCourseCatalogItem> =
        runCatching {
            val raw = prefs.getString("items", null) ?: return emptyList()
            Gson().fromJson<List<ShenzhenCourseCatalogItem>>(
                raw,
                object : TypeToken<List<ShenzhenCourseCatalogItem>>() {}.type,
            ) ?: emptyList()
        }.getOrDefault(emptyList())

    private fun savePlan(prefs: android.content.SharedPreferences, items: List<ShenzhenCourseCatalogItem>) {
        prefs.edit().putString("items", Gson().toJson(items)).apply()
    }
}
