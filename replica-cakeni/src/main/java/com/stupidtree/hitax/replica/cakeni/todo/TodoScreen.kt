package com.stupidtree.hitax.replica.cakeni.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stupidtree.hita.api.TodoAccountState
import com.stupidtree.hita.api.TodoItem
import com.stupidtree.hita.api.TodoLoginState
import com.stupidtree.hita.api.TodoService
import com.stupidtree.hita.api.TodoSource
import com.stupidtree.hitax.replica.cakeni.CakeniDimens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/** 待办筛选范围。ALL 同时包含已完成条目，完成切换只走服务。 */
private enum class TodoRange(val label: String) {
    TODAY("今天"),
    WEEK("7天内"),
    ALL("全部"),
}

/** 列表分组，按声明顺序渲染。 */
private enum class TodoSection(val label: String) {
    OVERDUE("已逾期"),
    TODAY("今天"),
    TOMORROW("明天"),
    WEEK("未来7天"),
    LATER("稍后"),
    COMPLETED("已完成"),
}

/**
 * cakeni 风格待办页。
 *
 * 前置条件：可在任意组合环境调用；[todoService] 为 null 表示宿主未装载待办插件。
 * 后置条件：展示合并后的待办列表，按所选范围过滤、按截止时间分组；
 * 勾选只调用 [TodoService.setCompleted]，界面状态由 LiveData 回流驱动。
 * 不变式：本页不创建待办（TodoService 没有创建接口）；已完成条目只改本地状态。
 */
@Composable
fun TodoScreen(todoService: TodoService?, onOpenMarket: () -> Unit) {
    if (todoService == null) {
        TodoServiceMissing(onOpenMarket)
        return
    }
    TodoContent(todoService, onOpenMarket)
}

@Composable
private fun TodoContent(service: TodoService, onOpenMarket: () -> Unit) {
    val itemsLive = remember(service) { service.items() }
    val graderLive = remember(service) { service.state(TodoSource.GRADER) }
    val geniusLive = remember(service) { service.state(TodoSource.GENIUS_TA) }
    val loaded by itemsLive.observeAsState(initial = null)
    val grader by graderLive.observeAsState(TodoAccountState(TodoSource.GRADER))
    val genius by geniusLive.observeAsState(TodoAccountState(TodoSource.GENIUS_TA))

    var range by rememberSaveable { mutableStateOf(TodoRange.ALL) }
    var busy by remember { mutableStateOf(emptySet<String>()) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val allItems = loaded ?: emptyList()
    val now = System.currentTimeMillis()
    val todayStart = startOfDay(now)
    val tomorrowStart = plusDays(todayStart, 1)
    val dayAfterStart = plusDays(todayStart, 2)
    val weekEnd = plusDays(todayStart, 7)
    val pendingItems = allItems.filter { !it.completed }
    val completedCount = allItems.size - pendingItems.size
    val counts = mapOf(
        TodoRange.TODAY to pendingItems.count { it.dueAt < tomorrowStart },
        TodoRange.WEEK to pendingItems.count { it.dueAt >= now && it.dueAt < weekEnd },
        TodoRange.ALL to allItems.size,
    )
    val loggedOut = grader.loginState == TodoLoginState.LOGGED_OUT &&
        genius.loginState == TodoLoginState.LOGGED_OUT
    val filtered = when (range) {
        TodoRange.TODAY -> pendingItems.filter { it.dueAt < tomorrowStart }
        TodoRange.WEEK -> pendingItems.filter { it.dueAt >= now && it.dueAt < weekEnd }
        TodoRange.ALL -> allItems
    }
    val year = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR)
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()) }
    val fullFmt = remember { SimpleDateFormat("yyyy/M/d HH:mm", Locale.getDefault()) }

    fun toggle(item: TodoItem) {
        val key = "${item.source.name}:${item.id}"
        if (key in busy) return
        busy = busy + key
        scope.launch {
            val ok = runCatching {
                service.setCompleted(item.source, item.id, !item.completed)
            }.getOrDefault(false)
            busy = busy - key
            if (!ok) {
                snackbar.showSnackbar("切换完成状态失败：来源未登录、条目不存在或网络错误")
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TodoHeader(
            pending = pendingItems.size,
            completed = completedCount,
            total = allItems.size,
            onOpenMarket = onOpenMarket,
        )
        FilterBar(range, counts) { range = it }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.weight(1f)) {
            if (loaded == null) {
                LoadingState()
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = CakeniDimens.page,
                        end = CakeniDimens.page,
                        bottom = 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(CakeniDimens.gap),
                ) {
                    if (loggedOut && allItems.isNotEmpty()) {
                        item(key = "logged-out") {
                            StateCard(
                                title = "待办来源未登录",
                                message = "列表来自历史缓存；登录后可继续同步。",
                                actionLabel = "插件市场",
                                onAction = onOpenMarket,
                            )
                        }
                    }
                    if (filtered.isEmpty()) {
                        item(key = "empty") { EmptyState(range, loggedOut, onOpenMarket) }
                    } else {
                        for (section in TodoSection.entries) {
                            val sectionItems = filtered.filter {
                                sectionOf(it, now, tomorrowStart, dayAfterStart, weekEnd) == section
                            }
                            if (sectionItems.isEmpty()) continue
                            item(key = "header-${section.name}") {
                                SectionHeader(section.label, sectionItems.size)
                            }
                            items(sectionItems, key = { "${it.source.name}:${it.id}" }) { todo ->
                                TodoRow(
                                    item = todo,
                                    dueText = formatDue(
                                        dueAt = todo.dueAt,
                                        todayStart = todayStart,
                                        tomorrowStart = tomorrowStart,
                                        dayAfterStart = dayAfterStart,
                                        year = year,
                                        timeFmt = timeFmt,
                                        dateFmt = dateFmt,
                                        fullFmt = fullFmt,
                                    ),
                                    overdue = !todo.completed && todo.dueAt < now,
                                    busy = "${todo.source.name}:${todo.id}" in busy,
                                    onToggle = { toggle(todo) },
                                )
                            }
                        }
                    }
                }
            }
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter)
                    .padding(horizontal = CakeniDimens.page, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun TodoServiceMissing(onOpenMarket: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TodoHeader(pending = 0, completed = 0, total = 0, onOpenMarket = onOpenMarket)
        Box(
            Modifier.weight(1f).padding(horizontal = CakeniDimens.page, vertical = 16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            StateCard(
                title = "待办服务不可用",
                message = "需要宿主 API 11 且已安装待办插件（plugin-todo，服务键 hita.todo）。",
                actionLabel = "打开插件市场",
                onAction = onOpenMarket,
            )
        }
    }
}

@Composable
private fun TodoHeader(pending: Int, completed: Int, total: Int, onOpenMarket: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .padding(start = CakeniDimens.page, end = 8.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "待办",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "$pending 项待完成 · 已完成 $completed / 共 $total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onOpenMarket) { Text("插件市场") }
    }
}

@Composable
private fun FilterBar(
    selected: TodoRange,
    counts: Map<TodoRange, Int>,
    onSelect: (TodoRange) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = CakeniDimens.page),
    ) {
        Row(Modifier.padding(4.dp)) {
            for (range in TodoRange.entries) {
                val active = range == selected
                Box(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                        )
                        .clickable { onSelect(range) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${range.label} ${counts[range] ?: 0}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (active) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String, count: Int) {
    Text(
        "$label · $count",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
    )
}

@Composable
private fun TodoRow(
    item: TodoItem,
    dueText: String,
    overdue: Boolean,
    busy: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(CakeniDimens.cardRadius),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 6.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = item.completed,
                onCheckedChange = { onToggle() },
                enabled = !busy,
            )
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    item.title.ifBlank { "未命名待办" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (item.completed) TextDecoration.LineThrough else null,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (item.completed) 0.6f else 1f,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SourceTag(item.source)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        item.courseTitle.ifBlank { "未关联课程" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        dueText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (overdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceTag(source: TodoSource) {
    val container = when (source) {
        TodoSource.GRADER -> MaterialTheme.colorScheme.primaryContainer
        TodoSource.GENIUS_TA -> MaterialTheme.colorScheme.secondaryContainer
    }
    val ink = when (source) {
        TodoSource.GRADER -> MaterialTheme.colorScheme.onPrimaryContainer
        TodoSource.GENIUS_TA -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    Text(
        sourceLabel(source),
        style = MaterialTheme.typography.labelSmall,
        color = ink,
        maxLines = 1,
        modifier = Modifier
            .background(container, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun StateCard(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(CakeniDimens.cardRadius),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun EmptyState(range: TodoRange, loggedOut: Boolean, onOpenMarket: () -> Unit) {
    if (loggedOut) {
        StateCard(
            title = "尚未登录待办来源",
            message = "登录 Grader 或 GeniusTA 后即可同步作业与截止时间。",
            actionLabel = "打开插件市场",
            onAction = onOpenMarket,
        )
        return
    }
    val (title, message) = when (range) {
        TodoRange.TODAY -> "今天暂时没有截止事项" to "给自己留一点从容。"
        TodoRange.WEEK -> "未来 7 天暂无截止事项" to "逾期事项可在「全部」中查看。"
        TodoRange.ALL -> "暂无待办" to "登录来源并同步后，作业会显示在这里。"
    }
    StateCard(title = title, message = message)
}

@Composable
private fun LoadingState() {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(
            "正在加载待办…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun sourceLabel(source: TodoSource): String = when (source) {
    TodoSource.GRADER -> "Grader"
    TodoSource.GENIUS_TA -> "GeniusTA"
}

/**
 * 分组判定。
 *
 * 前置条件：todayStart/tomorrowStart/dayAfterStart/weekEnd 由同一 now 推出的当天零点。
 * 后置条件：已完成条目归入 COMPLETED；未完成按逾期、今天、明天、7 天内、稍后归类。
 * 不变式：纯函数，不读取系统时间。
 */
private fun sectionOf(
    item: TodoItem,
    now: Long,
    tomorrowStart: Long,
    dayAfterStart: Long,
    weekEnd: Long,
): TodoSection {
    if (item.completed) return TodoSection.COMPLETED
    if (item.dueAt < now) return TodoSection.OVERDUE
    val day = startOfDay(item.dueAt)
    return when {
        day < tomorrowStart -> TodoSection.TODAY
        day < dayAfterStart -> TodoSection.TOMORROW
        item.dueAt < weekEnd -> TodoSection.WEEK
        else -> TodoSection.LATER
    }
}

/** 截止时间文案：今天/明天只给时刻，同年省年份，跨年给完整日期。 */
private fun formatDue(
    dueAt: Long,
    todayStart: Long,
    tomorrowStart: Long,
    dayAfterStart: Long,
    year: Int,
    timeFmt: SimpleDateFormat,
    dateFmt: SimpleDateFormat,
    fullFmt: SimpleDateFormat,
): String {
    val date = Date(dueAt)
    val day = startOfDay(dueAt)
    val dueYear = Calendar.getInstance().apply { timeInMillis = dueAt }.get(Calendar.YEAR)
    return when {
        day < tomorrowStart -> "今天 ${timeFmt.format(date)}"
        day < dayAfterStart -> "明天 ${timeFmt.format(date)}"
        dueYear == year -> dateFmt.format(date)
        else -> fullFmt.format(date)
    }
}

private fun startOfDay(ts: Long): Long = Calendar.getInstance().apply {
    timeInMillis = ts
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun plusDays(ts: Long, days: Int): Long = Calendar.getInstance().apply {
    timeInMillis = ts
    add(Calendar.DAY_OF_YEAR, days)
}.timeInMillis
