package com.stupidtree.hita.ui.group

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
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.Timetable
import org.json.JSONArray
import java.text.SimpleDateFormat

class GroupUiPlugin : ComposeUiPlugin {
    override val id = "com.stupidtree.hita.ui.group"
    override val displayName = "课表组合"
    override val requiresApi = 7

    override fun createView(context: Context, host: PluginUiHost): View {
        val prefs = context.getSharedPreferences("timetable_group", Context.MODE_PRIVATE)
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    val service = host.service(TimetableService.KEY)
                    val timetables by (service?.getTimetables()
                        ?: androidx.lifecycle.MutableLiveData(emptyList()))
                        .observeAsState(emptyList())
                    var group by remember { mutableStateOf(loadGroup(prefs)) }
                    var active by remember { mutableStateOf<String?>(null) }
                    var merged by remember { mutableStateOf(false) }

                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text("课表组合", style = MaterialTheme.typography.h5)
                        Text("勾选课表组成组合，组内切换或合并显示。", style = MaterialTheme.typography.caption)
                        LazyColumn(Modifier.padding(top = 8.dp)) {
                            items(timetables) { timetable ->
                                Row(Modifier.fillMaxWidth().clickable {
                                    group = if (timetable.id in group) {
                                        group - timetable.id
                                    } else group + timetable.id
                                    saveGroup(prefs, group)
                                    if (active !in group) active = group.firstOrNull()
                                }.padding(vertical = 4.dp)) {
                                    Checkbox(checked = timetable.id in group, onCheckedChange = null)
                                    Text(timetable.name ?: "未命名", modifier = Modifier.padding(top = 12.dp))
                                }
                            }
                        }
                        if (group.isEmpty()) {
                            Text("尚未组合课表", modifier = Modifier.padding(top = 12.dp))
                        } else {
                            Row(Modifier.padding(top = 12.dp)) {
                                Button(onClick = { merged = !merged }) {
                                    Text(if (merged) "切换视图" else "合并显示")
                                }
                            }
                            val visible = if (merged) group else listOfNotNull(active ?: group.first())
                            Row(Modifier.padding(top = 8.dp)) {
                                group.forEach { id ->
                                    val title = timetables.firstOrNull { it.id == id }?.name ?: id
                                    Button(
                                        onClick = { active = id; merged = false },
                                        modifier = Modifier.padding(end = 6.dp),
                                    ) { Text(title) }
                                }
                            }
                            Events(service, timetables, visible, merged)
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Events(
        service: TimetableService?,
        timetables: List<Timetable>,
        ids: List<String>,
        merged: Boolean,
    ) {
        val events by produceState(emptyList<EventItem>(), ids, merged) {
            value = withContext(Dispatchers.IO) {
                ids.flatMap { id ->
                    timetables.firstOrNull { it.id == id }?.let { load(service, it) }.orEmpty()
                }.sortedBy { it.from.time }
            }
        }
        val fmt = SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        LazyColumn(Modifier.padding(top = 8.dp)) {
            items(events) { event ->
                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Text(event.name)
                        Text("${fmt.format(event.from)} - ${fmt.format(event.to)} · ${event.place.orEmpty()}",
                            style = MaterialTheme.typography.caption)
                    }
                }
            }
        }
    }

    private fun load(service: TimetableService?, timetable: Timetable): List<EventItem> {
        if (service == null) return emptyList()
        val result = mutableListOf<EventItem>()
        val latch = java.util.concurrent.CountDownLatch(1)
        val live = service.getEventsDuring(timetable.startTime.time, timetable.endTime.time)
        val observer = object : androidx.lifecycle.Observer<List<EventItem>> {
            override fun onChanged(value: List<EventItem>) {
                result.clear()
                result += value.filter { it.timetableId == timetable.id && it.type == EventItem.TYPE.CLASS }
                live.removeObserver(this)
                latch.countDown()
            }
        }
        android.os.Handler(android.os.Looper.getMainLooper()).post { live.observeForever(observer) }
        latch.await(10, java.util.concurrent.TimeUnit.SECONDS)
        return result
    }

    private fun loadGroup(prefs: android.content.SharedPreferences): List<String> = runCatching {
        val arr = JSONArray(prefs.getString("ids", "[]"))
        (0 until arr.length()).map { arr.getString(it) }
    }.getOrDefault(emptyList())

    private fun saveGroup(prefs: android.content.SharedPreferences, ids: List<String>) {
        prefs.edit().putString("ids", JSONArray(ids).toString()).apply()
    }
}
