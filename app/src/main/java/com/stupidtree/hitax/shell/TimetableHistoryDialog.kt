package com.stupidtree.hitax.shell

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stupidtree.hitax.data.model.timetable.EventItem
import java.text.DateFormat
import java.util.Date

@Composable
fun TimetableHistoryDialog(
    versions: List<TimetableVersionRecord>,
    currentEvents: List<EventItem>,
    onDismiss: () -> Unit,
    onRestore: (TimetableVersionRecord) -> Unit,
) {
    var selected by remember(versions) { mutableStateOf<TimetableVersionRecord?>(null) }
    var confirmRestore by remember { mutableStateOf(false) }
    val background = MaterialTheme.colorScheme.background
    val foreground = MaterialTheme.colorScheme.onSurface
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().systemBarsPadding(),
            color = background,
        ) {
            CompositionLocalProvider(LocalContentColor provides foreground) {
                Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "教务版本",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { if (selected == null) onDismiss() else selected = null }) {
                        Text(if (selected == null) "关闭" else "返回")
                    }
                }
                Text(
                    "仅恢复教务课程；个人日程、隐藏状态、考试和外观设置保留",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "保留最近 5 个版本（包含当前版）",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (selected == null) {
                    LazyColumn(
                        Modifier.weight(1f).padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(versions, key = { it.id }) { version ->
                            VersionCard(
                                version = version,
                                current = version.id == versions.firstOrNull()?.id,
                                onCompare = { selected = version },
                            )
                        }
                    }
                } else {
                    val version = selected!!
                    VersionComparison(
                        version = version,
                        currentRevision = versions.firstOrNull()?.revision ?: version.revision,
                        diff = TimetableHistoryDiff.diff(version.events, currentEvents),
                        onRestore = { confirmRestore = true },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
    }
    if (confirmRestore) {
        val version = selected
        if (version != null) {
            AlertDialog(
                onDismissRequest = { confirmRestore = false },
                title = { Text("恢复这个课表版本？") },
                text = { Text("当前教务课程会先保存为“恢复前”版本，然后替换为 v${version.revision}；个人活动和考试不受影响。") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmRestore = false
                        onRestore(version)
                    }) { Text("恢复") }
                },
                dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("取消") } },
            )
        }
    }
}

@Composable
private fun VersionCard(
    version: TimetableVersionRecord,
    current: Boolean,
    onCompare: () -> Unit,
) {
    OutlinedCard(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${if (current) "当前版本" else "历史版本"} · v${version.revision}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text("记录时间：${formatTime(version.committedAt)}")
            Text(reasonLabel(version.reason), style = MaterialTheme.typography.bodySmall)
            if (!current) {
                TextButton(onClick = onCompare) { Text("对比此版本") }
            }
        }
    }
}

@Composable
private fun VersionComparison(
    version: TimetableVersionRecord,
    currentRevision: Int,
    diff: TimetableDiff,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            "对比 v${version.revision} → 当前 v$currentRevision",
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(versionDiffSummary(diff), style = MaterialTheme.typography.labelLarge)
        LazyColumn(
            Modifier.weight(1f).padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(diff.changes) { change -> VersionChangeCard(change) }
        }
        Button(
            onClick = onRestore,
            enabled = !diff.isEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("恢复这个版本") }
    }
}

@Composable
private fun VersionChangeCard(change: TimetableChange) {
    val lesson = change.after ?: change.before
    if (lesson == null) return
    val label = when (change.kind) {
        TimetableChangeKind.ADDED -> "新增"
        TimetableChangeKind.REMOVED -> "删除"
        TimetableChangeKind.MOVED -> "移动"
        TimetableChangeKind.DETAILS -> "详情变化"
    }
    OutlinedCard(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$label · ${lesson.name}", style = MaterialTheme.typography.titleSmall)
            change.before?.let {
                Text("− ${formatTime(it.from.time)} – ${formatTime(it.to.time)} ${it.place.orEmpty()}")
            }
            change.after?.let {
                Text("＋ ${formatTime(it.from.time)} – ${formatTime(it.to.time)} ${it.place.orEmpty()}")
            }
        }
    }
}

private fun versionDiffSummary(diff: TimetableDiff): String {
    val parts = buildList {
        diff.count(TimetableChangeKind.MOVED).takeIf { it > 0 }?.let { add("移动 $it 节") }
        diff.count(TimetableChangeKind.REMOVED).takeIf { it > 0 }?.let { add("删除 $it 节") }
        diff.count(TimetableChangeKind.ADDED).takeIf { it > 0 }?.let { add("新增 $it 节") }
        diff.count(TimetableChangeKind.DETAILS).takeIf { it > 0 }?.let { add("详情 $it 节") }
    }
    return parts.joinToString(" · ").ifEmpty { "没有差异" }
}

private fun formatTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(millis))

private fun reasonLabel(reason: String): String = when (reason) {
    "INITIAL" -> "初始版本"
    "RESTORE_BACKUP" -> "恢复前"
    "RESTORE" -> "已恢复"
    else -> "教务更新"
}

