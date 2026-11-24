package com.stupidtree.hita.plugin.todoui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.TodoAccountState
import com.stupidtree.hita.api.TodoError
import com.stupidtree.hita.api.TodoItem
import com.stupidtree.hita.api.TodoLoginState
import com.stupidtree.hita.api.TodoService
import com.stupidtree.hita.api.TodoSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * 待办主界面。
 *
 * 前置条件：[service] 由宿主注入且调用发生在主线程。
 * 后置条件：列表与会话状态通过 LiveData 订阅；所有写操作经界面按钮触发。
 * 不变式：只展示 [TodoSource] 两个来源；关闭由 [PluginUiHost.close] 完成。
 */
@Composable
internal fun TodoScreen(host: PluginUiHost, service: TodoService) {
    val graderLive = remember(service) { service.state(TodoSource.GRADER) }
    val geniusLive = remember(service) { service.state(TodoSource.GENIUS_TA) }
    val itemsLive = remember(service) { service.items() }

    val graderState by graderLive.observeAsState(TodoAccountState(TodoSource.GRADER))
    val geniusState by geniusLive.observeAsState(TodoAccountState(TodoSource.GENIUS_TA))
    val allItems by itemsLive.observeAsState(emptyList())

    var showAccounts by remember { mutableStateOf(true) }
    var activeLogin by remember { mutableStateOf<TodoSource?>(null) }
    var showGraderWeb by remember { mutableStateOf(false) }
    var busySource by remember { mutableStateOf<TodoSource?>(null) }
    var refreshingAll by remember { mutableStateOf(false) }
    var toggling by remember { mutableStateOf(setOf<String>()) }

    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val dueFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    fun stateOf(source: TodoSource): TodoAccountState =
        if (source == TodoSource.GRADER) graderState else geniusState

    fun notify(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    fun refresh(source: TodoSource) {
        if (busySource != null || refreshingAll) return
        scope.launch {
            busySource = source
            val ok = try {
                when (source) {
                    TodoSource.GRADER -> service.fetchGraderAssignments()
                    TodoSource.GENIUS_TA -> service.importGeniusTaAssignments()
                }
            } finally {
                busySource = null
            }
            notify(
                if (ok) "${sourceLabel(source)} 已刷新"
                else "${sourceLabel(source)} 刷新失败：未登录、会话过期或网络错误"
            )
        }
    }

    fun refreshAll() {
        if (busySource != null || refreshingAll) return
        scope.launch {
            refreshingAll = true
            try {
                val sources = TodoSource.entries.filter {
                    stateOf(it).loginState != TodoLoginState.LOGGED_OUT
                }
                if (sources.isEmpty()) {
                    notify("尚未登录任何来源，请先在账号面板登录")
                    return@launch
                }
                var succeeded = 0
                for (source in sources) {
                    val ok = when (source) {
                        TodoSource.GRADER -> service.fetchGraderAssignments()
                        TodoSource.GENIUS_TA -> service.importGeniusTaAssignments()
                    }
                    if (ok) succeeded++
                }
                notify(
                    when {
                        succeeded == sources.size -> "已刷新 ${sources.size} 个来源"
                        succeeded == 0 -> "刷新失败，请查看账号状态"
                        else -> "部分来源刷新失败"
                    }
                )
            } finally {
                refreshingAll = false
            }
        }
    }

    fun logout(source: TodoSource) {
        if (busySource != null || refreshingAll) return
        scope.launch {
            busySource = source
            val ok = try {
                service.logout(source)
            } finally {
                busySource = null
            }
            notify(if (ok) "${sourceLabel(source)} 已退出登录" else "退出登录失败")
        }
    }

    fun toggle(item: TodoItem, completed: Boolean) {
        val key = "${item.source.name}/${item.id}"
        if (key in toggling) return
        toggling = toggling + key
        scope.launch {
            val ok = try {
                service.setCompleted(item.source, item.id, completed)
            } finally {
                toggling = toggling - key
            }
            if (!ok) notify("更新失败：来源未登录、条目不存在或已提交")
        }
    }

    val sortedItems = allItems.sortedWith(compareBy<TodoItem>({ it.dueAt }, { it.title }))
    val now = System.currentTimeMillis()
    val overdue = sortedItems.filter { !it.completed && it.dueAt < now }
    val upcoming = sortedItems.filter { !it.completed && it.dueAt >= now }
    val completed = sortedItems.filter { it.completed }
    val unfinished = sortedItems.size - completed.size

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TodoHeader(
                unfinishedCount = unfinished,
                totalCount = sortedItems.size,
                showAccounts = showAccounts,
                refreshing = refreshingAll || busySource != null,
                onToggleAccounts = { showAccounts = !showAccounts },
                onRefresh = { refreshAll() },
                onClose = { host.close() },
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                if (showAccounts) {
                    item(key = "accounts") {
                        AccountSection(
                            grader = graderState,
                            genius = geniusState,
                            busySource = busySource,
                            refreshingAll = refreshingAll,
                            onLogin = { source ->
                                activeLogin = source
                                showGraderWeb = source == TodoSource.GRADER
                            },
                            onRefresh = { refresh(it) },
                            onLogout = { logout(it) },
                        )
                    }
                }
                if (sortedItems.isEmpty()) {
                    item(key = "empty") { EmptyState() }
                } else {
                    if (overdue.isNotEmpty()) {
                        item(key = "overdue-header") {
                            SectionHeader("已逾期", overdue.size, MaterialTheme.colorScheme.error)
                        }
                        items(overdue, key = { "${it.source.name}/overdue/${it.id}" }) { item ->
                            TodoRow(
                                item = item,
                                overdue = true,
                                busy = "${item.source.name}/${item.id}" in toggling,
                                dueFormat = dueFormat,
                                onToggle = { toggle(item, it) },
                            )
                        }
                    }
                    if (upcoming.isNotEmpty()) {
                        item(key = "upcoming-header") {
                            SectionHeader("待完成", upcoming.size)
                        }
                        items(upcoming, key = { "${it.source.name}/upcoming/${it.id}" }) { item ->
                            TodoRow(
                                item = item,
                                overdue = false,
                                busy = "${item.source.name}/${item.id}" in toggling,
                                dueFormat = dueFormat,
                                onToggle = { toggle(item, it) },
                            )
                        }
                    }
                    if (completed.isNotEmpty()) {
                        item(key = "completed-header") {
                            SectionHeader("已完成", completed.size)
                        }
                        items(completed, key = { "${it.source.name}/completed/${it.id}" }) { item ->
                            TodoRow(
                                item = item,
                                overdue = false,
                                busy = "${item.source.name}/${item.id}" in toggling,
                                dueFormat = dueFormat,
                                onToggle = { toggle(item, it) },
                            )
                        }
                    }
                }
            }
        }
    }

    activeLogin?.let { source ->
        val dismiss = {
            activeLogin = null
            showGraderWeb = false
        }
        when (source) {
            TodoSource.GRADER -> if (showGraderWeb) {
                GraderWebLoginDialog(
                    service = service,
                    onDismiss = { showGraderWeb = false },
                    onSuccess = {
                        activeLogin = null
                        showGraderWeb = false
                        notify("Grader 会话已连接，正在导入作业")
                    },
                )
            } else {
                GraderLoginDialog(
                    service = service,
                    onDismiss = dismiss,
                    onOpenWebLogin = { showGraderWeb = true },
                    onSuccess = {
                        activeLogin = null
                        notify("Grader 会话已连接，正在导入作业")
                    },
                )
            }
            TodoSource.GENIUS_TA -> GeniusTaLoginDialog(
                service = service,
                onDismiss = dismiss,
                onSuccess = {
                    activeLogin = null
                    notify("GeniusTA 登录成功，正在导入作业")
                },
            )
        }
    }
}

@Composable
private fun TodoHeader(
    unfinishedCount: Int,
    totalCount: Int,
    showAccounts: Boolean,
    refreshing: Boolean,
    onToggleAccounts: () -> Unit,
    onRefresh: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("待办", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "$unfinishedCount 条未完成 · 共 $totalCount 条 · Grader + GeniusTA",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onToggleAccounts) {
                Text(if (showAccounts) "收起账号" else "账号")
            }
            TextButton(onClick = onRefresh, enabled = !refreshing) {
                Text(if (refreshing) "刷新中" else "刷新")
            }
            TextButton(onClick = onClose) { Text("关闭") }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("暂无待办", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "登录 Grader 或 GeniusTA 后点刷新；逾期与已完成条目会分组显示。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AccountSection(
    grader: TodoAccountState,
    genius: TodoAccountState,
    busySource: TodoSource?,
    refreshingAll: Boolean,
    onLogin: (TodoSource) -> Unit,
    onRefresh: (TodoSource) -> Unit,
    onLogout: (TodoSource) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            "账号与来源",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        SourceAccountCard(
            state = grader,
            busy = busySource == TodoSource.GRADER || refreshingAll,
            onLogin = { onLogin(TodoSource.GRADER) },
            onRefresh = { onRefresh(TodoSource.GRADER) },
            onLogout = { onLogout(TodoSource.GRADER) },
        )
        Spacer(Modifier.height(8.dp))
        SourceAccountCard(
            state = genius,
            busy = busySource == TodoSource.GENIUS_TA || refreshingAll,
            onLogin = { onLogin(TodoSource.GENIUS_TA) },
            onRefresh = { onRefresh(TodoSource.GENIUS_TA) },
            onLogout = { onLogout(TodoSource.GENIUS_TA) },
        )
    }
}

@Composable
private fun SourceAccountCard(
    state: TodoAccountState,
    busy: Boolean,
    onLogin: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SourceBadge(state.source)
                Spacer(Modifier.width(8.dp))
                Text(
                    loginStateLabel(state.loginState),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = loginStateColor(state.loginState),
                )
                Spacer(Modifier.weight(1f))
                if (state.syncing) {
                    Text(
                        "同步中…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "账号：${state.accountName ?: state.accountId ?: "未绑定"}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "缓存 ${state.itemCount} 条待办",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            errorText(state.error)?.let {
                Text(
                    "错误：$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.source == TodoSource.GRADER) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Grader 使用学校 CAS 网页登录；可在应用内打开登录页，或粘贴 Cookie 兜底。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (state.loginState) {
                    TodoLoginState.LOGGED_OUT -> Button(onClick = onLogin, enabled = !busy) {
                        Text(
                            if (state.source == TodoSource.GRADER) "网页登录"
                            else "账号密码登录"
                        )
                    }
                    TodoLoginState.EXPIRED -> {
                        Button(onClick = onLogin, enabled = !busy) {
                            Text(
                                if (state.source == TodoSource.GRADER) "重新网页登录"
                                else "重新登录"
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = onLogout, enabled = !busy) {
                            Text("退出登录")
                        }
                    }
                    TodoLoginState.ACTIVE -> {
                        OutlinedButton(onClick = onRefresh, enabled = !busy) {
                            Text(if (state.syncing) "同步中…" else "立即刷新")
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = onLogout, enabled = !busy) {
                            Text("退出登录")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoRow(
    item: TodoItem,
    overdue: Boolean,
    busy: Boolean,
    dueFormat: SimpleDateFormat,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = item.completed,
                onCheckedChange = onToggle,
                enabled = !busy,
            )
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceBadge(item.source)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        item.courseTitle.ifBlank { "未分类课程" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (item.completed) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    buildString {
                        append("截止 ")
                        append(dueFormat.format(Date(item.dueAt)))
                        if (item.completed) append(" · 已完成")
                        else if (overdue) append(" · 已逾期")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue && !item.completed) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.width(6.dp))
        Text("$count", style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
internal fun SourceBadge(source: TodoSource) {
    val container = when (source) {
        TodoSource.GRADER -> MaterialTheme.colorScheme.primaryContainer
        TodoSource.GENIUS_TA -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val content = when (source) {
        TodoSource.GRADER -> MaterialTheme.colorScheme.onPrimaryContainer
        TodoSource.GENIUS_TA -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(4.dp)) {
        Text(
            sourceLabel(source),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/** 服务缺失时的兜底界面：说明依赖，仍允许退出。 */
@Composable
internal fun TodoUnavailable(host: PluginUiHost) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("待办服务不可用", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "需要宿主 API 11 且已安装待办插件（plugin-todo，服务键 hita.todo）。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { host.close() }) { Text("关闭") }
    }
}

internal fun sourceLabel(source: TodoSource): String = when (source) {
    TodoSource.GRADER -> "Grader"
    TodoSource.GENIUS_TA -> "GeniusTA"
}

internal fun loginStateLabel(state: TodoLoginState): String = when (state) {
    TodoLoginState.LOGGED_OUT -> "未登录"
    TodoLoginState.ACTIVE -> "已登录"
    TodoLoginState.EXPIRED -> "会话过期，需重新登录"
}

@Composable
private fun loginStateColor(state: TodoLoginState): Color = when (state) {
    TodoLoginState.LOGGED_OUT -> MaterialTheme.colorScheme.onSurfaceVariant
    TodoLoginState.ACTIVE -> MaterialTheme.colorScheme.primary
    TodoLoginState.EXPIRED -> MaterialTheme.colorScheme.error
}

internal fun errorText(error: TodoError): String? = when (error) {
    TodoError.NONE -> null
    TodoError.AUTHENTICATION -> "认证失败或会话已过期"
    TodoError.NETWORK -> "网络错误，请稍后重试"
    TodoError.STORAGE -> "本地缓存读写失败"
    TodoError.INVALID_RESPONSE -> "远端返回无法解析"
}
