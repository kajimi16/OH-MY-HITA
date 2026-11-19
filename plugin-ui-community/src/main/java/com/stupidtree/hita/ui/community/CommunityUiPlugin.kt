package com.stupidtree.hita.ui.community

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.Observer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.CourseCatalogService
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hitax.data.model.eas.TermItem
import com.stupidtree.hitax.data.model.timetable.TermSubject

/**
 * 深圳课程浏览：读取教务课程池的原生列表。
 * 选课动作、附件、历史挂科率尚未移植；连接 Web 教务入口保留。
 */
class CommunityUiPlugin : ComposeUiPlugin {
    override val id = "com.stupidtree.hita.ui.community"
    override val displayName = "深圳课程浏览"
    override val requiresApi = 7

    override fun createView(context: Context, host: PluginUiHost): View {
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    val service = remember { host.service(CourseCatalogService.KEY) }
                    var reload by remember { mutableIntStateOf(0) }
                    var page by remember { mutableIntStateOf(1) }
                    var selectedTerm by remember { mutableStateOf<TermItem?>(null) }
                    var keyword by remember { mutableStateOf("") }
                    var appliedKeyword by remember { mutableStateOf("") }
                    var status by remember { mutableStateOf("") }
                    var detailCourse by remember { mutableStateOf<TermSubject?>(null) }
                    var detailText by remember { mutableStateOf("") }
                    val termsLive = remember(service, reload) { service?.terms() }
                    val termsState = termsLive?.observeAsState()?.value
                    LaunchedEffect(termsState, reload) {
                        val state = termsState
                        if (state?.state == DataState.STATE.SUCCESS) {
                            val terms = state.data.orEmpty()
                            if (selectedTerm == null) {
                                selectedTerm = terms.firstOrNull { it.isCurrent } ?: terms.firstOrNull()
                            }
                        }
                    }
                    val coursesLive = remember(service, selectedTerm, page, reload, appliedKeyword) {
                        selectedTerm?.let {
                            service?.courses(it, page = page, pageSize = 20, keyword = appliedKeyword)
                        }
                    }
                    val coursesState = coursesLive?.observeAsState()?.value

                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { reload++ }) { Text("刷新") }
                        }
                        if (service == null) {
                            Text("教务插件未安装", Modifier.padding(24.dp))
                            return@Column
                        }
                        val loginRequired = termsState?.state == DataState.STATE.NOT_LOGGED_IN ||
                            termsState?.state == DataState.STATE.TOKEN_INVALID
                        if (loginRequired) {
                            Column(Modifier.padding(16.dp)) {
                                Text("需要连接深圳 Web 教务才能浏览课程池。")
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent().setClassName(
                                                context,
                                                "com.stupidtree.hitax.ui.eas.login.ShenzhenWebLoginActivity",
                                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    }
                                }) { Text("连接 Web 教务") }
                            }
                            return@Column
                        }
                        selectedTerm?.let { term ->
                            Text(
                                term.name,
                                fontSize = 14.sp,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextField(
                                value = keyword,
                                onValueChange = { keyword = it },
                                placeholder = { Text("课程名 / 教师") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                appliedKeyword = keyword.trim()
                                page = 1
                                reload++
                            }) { Text("搜索") }
                        }
                        when {
                            termsState == null || termsState?.state == DataState.STATE.NOTHING ->
                                CircularProgressIndicator(Modifier.padding(24.dp))
                            termsState?.state == DataState.STATE.FETCH_FAILED -> Column(Modifier.padding(16.dp)) {
                                Text("课程池加载失败：${termsState?.message.orEmpty()}")
                                TextButton(onClick = { reload++ }) { Text("重试") }
                            }
                            selectedTerm == null -> Text("没有可用学期", Modifier.padding(16.dp))
                            else -> {
                                when {
                                    coursesState == null || coursesState?.state == DataState.STATE.NOTHING ->
                                        CircularProgressIndicator(Modifier.padding(24.dp))
                                    coursesState?.state == DataState.STATE.FETCH_FAILED -> Column(Modifier.padding(16.dp)) {
                                        Text("课程加载失败：${coursesState?.message.orEmpty()}")
                                        TextButton(onClick = { reload++ }) { Text("重试") }
                                    }
                                    coursesState?.data.isNullOrEmpty() -> Text("本学期暂无课程", Modifier.padding(16.dp))
                                    else -> {
                                        LazyColumn(Modifier.weight(1f)) {
                                            items(coursesState?.data.orEmpty()) { course ->
                                                CatalogRow(course, onDetails = {
                                                    detailCourse = course
                                                    detailText = "正在加载课程详情…"
                                                    val live = service.attachments(course)
                                                    val observer = object : Observer<DataState<List<com.stupidtree.hita.api.CatalogAttachment>>> {
                                                        override fun onChanged(value: DataState<List<com.stupidtree.hita.api.CatalogAttachment>>) {
                                                            detailText = if (value.state == DataState.STATE.SUCCESS) {
                                                                value.data.orEmpty().joinToString("\n\n") { item ->
                                                                    "${item.kind}：${item.name}"
                                                                }.ifBlank { "暂无附件" }
                                                            } else {
                                                                "详情加载失败：${value.message ?: value.state}"
                                                            }
                                                            live.removeObserver(this)
                                                        }
                                                    }
                                                    live.observeForever(observer)
                                                }) {
                                                    val term = selectedTerm ?: return@CatalogRow
                                                    val live = service.selectCourse(term, course)
                                                    val observer = object : Observer<DataState<String>> {
                                                        override fun onChanged(value: DataState<String>) {
                                                            status = if (value.state == DataState.STATE.SUCCESS) {
                                                                "选课成功：${course.name}"
                                                            } else {
                                                                "选课失败：${value.message ?: value.state}"
                                                            }
                                                            live.removeObserver(this)
                                                        }
                                                    }
                                                    live.observeForever(observer)
                                                }
                                            }
                                        }
                                        if (status.isNotBlank()) {
                                            Text(status, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                                        }
                                        Row(
                                            Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            TextButton(
                                                enabled = page > 1,
                                                onClick = { page-- },
                                            ) { Text("上一页") }
                                            Text("第 $page 页", Modifier.align(Alignment.CenterVertically))
                                            TextButton(onClick = { page++ }) { Text("下一页") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    detailCourse?.let { course ->
                        AlertDialog(
                            onDismissRequest = { detailCourse = null },
                            title = { Text(course.name) },
                            text = { Text(detailText) },
                            confirmButton = {
                                TextButton(onClick = { detailCourse = null }) { Text("关闭") }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogRow(
    course: TermSubject,
    onDetails: () -> Unit,
    onSelect: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(course.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    buildString {
                        course.code?.takeIf { it.isNotBlank() }?.let { append(it).append(" · ") }
                        append("${course.credit} 学分")
                        course.school?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
                        course.field?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.68f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            TextButton(onClick = onDetails) { Text("详情") }
            TextButton(onClick = onSelect) { Text("选课") }
        }
    }
}
