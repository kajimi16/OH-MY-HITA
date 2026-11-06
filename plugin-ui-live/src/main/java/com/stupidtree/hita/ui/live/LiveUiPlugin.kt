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
import com.stupidtree.hita.api.LiveCourseService
import com.stupidtree.hita.api.LiveCourseSpec
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
                    val liveCourses = remember { host.service(LiveCourseService.KEY) }
                    var status by remember { mutableStateOf("") }
                    val events = remember { runCatching { timetable?.getTodayEventsSync().orEmpty() }.getOrDefault(emptyList()) }
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text("直播课", style = MaterialTheme.typography.h5)
                        Text("扫描今日课程中的线上链接；课前由宿主前台服务守护并可打开直播。",
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
                                                    val url = findUrl(event)
                                                    val ok = liveCourses?.schedule(
                                                        LiveCourseSpec(
                                                            ownerId = ownerId,
                                                            id = event.id.toString(),
                                                            title = event.name,
                                                            url = url.orEmpty(),
                                                            startAtMillis = event.from.time,
                                                            endAtMillis = event.to.time,
                                                            advanceMinutes = 5,
                                                        )
                                                    ) == true
                                                    status = if (ok) "已安排直播课守护：${event.name}" else "守护安排失败"
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
