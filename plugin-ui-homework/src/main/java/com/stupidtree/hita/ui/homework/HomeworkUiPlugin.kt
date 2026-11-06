package com.stupidtree.hita.ui.homework

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import java.time.LocalDate

class HomeworkUiPlugin : ComposeUiPlugin {
    override val id = "com.stupidtree.hita.ui.homework"
    override val displayName = "作业"
    override val requiresApi = 7

    override fun createView(context: Context, host: PluginUiHost): View =
        ComposeView(context).apply {
            setContent { MaterialTheme { HomeworkScreen(HomeworkStore(context)) } }
        }
}

@Composable
private fun HomeworkScreen(store: HomeworkStore) {
    var items by remember { mutableStateOf(store.load()) }
    var title by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh(next: List<HomeworkItem>) {
        items = next
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("作业", style = MaterialTheme.typography.h5)
        Text(
            "本地保存，不上传；未完成的作业按计划日期排序。",
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(200); error = null },
            label = { Text("作业内容") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it.take(10); error = null },
                label = { Text("计划日期 yyyy-MM-dd") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.66f),
            )
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    val raw = dateText.trim()
                    val date = if (raw.isEmpty()) null else HomeworkStore.normalizeDate(raw)
                    if (raw.isNotEmpty() && date == null) {
                        error = "日期格式应为 yyyy-MM-dd"
                    } else {
                        refresh(store.add(title, date))
                        title = ""
                        dateText = ""
                    }
                },
            ) { Text("添加") }
        }
        error?.let {
            Text(it, color = MaterialTheme.colors.error, style = MaterialTheme.typography.caption)
        }

        val pending = items.filterNot { it.completed }
        val done = items.filter { it.completed }
        if (items.isEmpty()) {
            Text("暂无作业", modifier = Modifier.padding(top = 24.dp))
        }
        Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            SectionTitle("未完成 · ${pending.size}")
            if (pending.isEmpty()) EmptyLine("今天没有待办")
            pending.groupBy { it.plannedDate }
                .entries
                .sortedWith { a, b -> compareDate(a.key, b.key) }
                .forEach { (date, list) ->
                    Text(
                        date?.let { formatDate(it) } ?: "未安排日期",
                        style = MaterialTheme.typography.subtitle2,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                    list.forEach { item ->
                        HomeworkRow(
                            item = item,
                            onToggle = { refresh(store.setCompleted(item.id, it)) },
                            onDelete = { refresh(store.delete(item.id)) },
                        )
                    }
                }
            if (done.isNotEmpty()) {
                SectionTitle("已完成 · ${done.size}")
                done.forEach { item ->
                    HomeworkRow(
                        item = item,
                        onToggle = { refresh(store.setCompleted(item.id, it)) },
                        onDelete = { refresh(store.delete(item.id)) },
                    )
                }
                TextButton(onClick = { refresh(store.clearCompleted()) }) {
                    Text("清除已完成")
                }
            }
        }
    }
}

private fun compareDate(a: String?, b: String?): Int = when {
    a == null && b == null -> 0
    a == null -> 1
    b == null -> -1
    else -> a.compareTo(b)
}

private fun formatDate(raw: String): String = raw

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.subtitle1, modifier = Modifier.padding(vertical = 6.dp))
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, style = MaterialTheme.typography.caption, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun HomeworkRow(item: HomeworkItem, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = item.completed, onCheckedChange = onToggle)
            TextButton(onClick = { onToggle(!item.completed) }) {
                Text(if (item.completed) "撤销" else "完成")
            }
            Text(
                item.title,
                modifier = Modifier.fillMaxWidth(0.66f),
                textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
            )
            TextButton(onClick = onDelete) { Text("删除") }
        }
    }
}
