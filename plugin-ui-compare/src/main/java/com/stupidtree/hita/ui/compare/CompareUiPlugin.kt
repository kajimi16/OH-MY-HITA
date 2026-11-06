package com.stupidtree.hita.ui.compare

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
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
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
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.Timetable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CompareUiPlugin : ComposeUiPlugin {
    override val id = "com.stupidtree.hita.ui.compare"
    override val displayName = "课表对比"
    override val requiresApi = 7

    override fun createView(context: Context, host: PluginUiHost): View {
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    val service = host.service(TimetableService.KEY)
                    val timetables by (service?.getTimetables()
                        ?: androidx.lifecycle.MutableLiveData(emptyList()))
                        .observeAsState(emptyList())
                    var selected by remember { mutableStateOf<List<String>>(emptyList()) }
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text("课表对比", style = MaterialTheme.typography.h5)
                        Text("选择两份课表，计算时间重叠的课程。", style = MaterialTheme.typography.caption)
                        LazyColumn(Modifier.padding(top = 8.dp)) {
                            items(timetables) { timetable ->
                                Row(Modifier.fillMaxWidth().clickable {
                                    selected = when {
                                        timetable.id in selected -> selected - timetable.id
                                        selected.size < 2 -> selected + timetable.id
                                        else -> selected
                                    }
                                }.padding(vertical = 4.dp)) {
                                    Checkbox(
                                        checked = timetable.id in selected,
                                        onCheckedChange = null,
                                    )
                                    Text(timetable.name ?: "未命名课表", modifier = Modifier.padding(top = 12.dp))
                                }
                            }
                        }
                        if (selected.size == 2) {
                            val a = timetables.first { it.id == selected[0] }
                            val b = timetables.first { it.id == selected[1] }
                            Text("重叠课程", style = MaterialTheme.typography.subtitle1,
                                modifier = Modifier.padding(top = 12.dp))
                            val overlaps = remember(a.id, b.id) { compare(service, a, b) }
                            if (overlaps.isEmpty()) Text("没有时间重叠")
                            else LazyColumn {
                                items(overlaps) { overlap ->
                                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                        Column(Modifier.padding(10.dp)) {
                                            Text("${overlap.first.name} × ${overlap.second.name}")
                                            val fmt = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                                            Text(
                                                "${fmt.format(overlap.first.from)} - ${fmt.format(overlap.first.to)}",
                                                style = MaterialTheme.typography.caption,
                                            )
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

    private fun compare(
        service: TimetableService?,
        a: Timetable,
        b: Timetable,
    ): List<Pair<EventItem, EventItem>> {
        if (service == null) return emptyList()
        val fmt = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        val eventsA = load(service, a)
        val eventsB = load(service, b)
        return eventsA.flatMap { left ->
            eventsB.filter { right ->
                left.from.time < right.to.time && right.from.time < left.to.time
            }.map { right -> left to right }
        }.take(50)
    }

    private fun load(service: TimetableService, timetable: Timetable): List<EventItem> {
        val events = mutableListOf<EventItem>()
        val live = service.getEventsDuring(timetable.startTime.time, timetable.endTime.time)
        val latch = java.util.concurrent.CountDownLatch(1)
        val observer = object : androidx.lifecycle.Observer<List<EventItem>> {
            override fun onChanged(value: List<EventItem>) {
                events.clear()
                events += value.filter { it.timetableId == timetable.id && it.type == EventItem.TYPE.CLASS }
                live.removeObserver(this)
                latch.countDown()
            }
        }
        android.os.Handler(android.os.Looper.getMainLooper()).post { live.observeForever(observer) }
        latch.await(10, java.util.concurrent.TimeUnit.SECONDS)
        return events
    }
}
