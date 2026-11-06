package com.stupidtree.hita.ui.live

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.ReminderService
import com.stupidtree.hita.api.ReminderSpec
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hitax.data.model.timetable.EventItem
import java.util.regex.Pattern

class LiveUiPlugin : ComposeUiPlugin {
    override val id = "com.stupidtree.hita.ui.live"
    override val displayName = "直播课"
    override val requiresApi = 7

    override fun createView(context: Context, host: PluginUiHost): View {
        val ownerId = id
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    val timetable = remember { host.service(TimetableService.KEY) }
                    val reminders = remember { host.service(ReminderService.KEY) }
                    var status by remember { mutableStateOf("") }
                    val events = remember { runCatching { timetable?.getTodayEventsSync().orEmpty() }.getOrDefault(emptyList()) }
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text("直播课", style = MaterialTheme.typography.h5)
                        Text("扫描今日课程中的线上链接；仅做提醒与打开，不含前台守护。",
                            style = MaterialTheme.typography.caption)
                        if (events.isEmpty()) {
                            Text("今日没有课程", modifier = Modifier.padding(top = 24.dp))
                        } else {
                            LazyColumn(Modifier.padding(top = 8.dp)) {
                                items(events) { event ->
                                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Column(Modifier.padding(10.dp)) {
                                            Text(event.name)
                                            Text("${event.place.orEmpty()} · ${event.from}",
                                                style = MaterialTheme.typography.caption)
                                            val url = findUrl(event)
                                            Row {
                                                Button(
                                                    enabled = url != null,
                                                    onClick = {
                                                        runCatching {
                                                            context.startActivity(
                                                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            )
                                                        }
                                                    },
                                                ) { Text("打开链接") }
                                                Button(onClick = {
                                                    val trigger = event.from.time - 5 * 60_000L
                                                    val ok = reminders?.schedule(
                                                        ReminderSpec(ownerId, event.id.toString(), trigger, "直播课即将开始", event.name)
                                                    ) == true
                                                    status = if (ok) "已安排 ${event.name} 提醒" else "提醒失败"
                                                }) { Text("提前 5 分钟提醒") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Text(status, style = MaterialTheme.typography.caption)
                    }
                }
            }
        }
    }

    private fun findUrl(event: EventItem): String? {
        val text = "${event.name} ${event.place.orEmpty()}"
        val matcher = URL.matcher(text)
        return if (matcher.find()) matcher.group() else null
    }

    private companion object {
        val URL: Pattern = Pattern.compile("https?://\\S+")
    }
}
