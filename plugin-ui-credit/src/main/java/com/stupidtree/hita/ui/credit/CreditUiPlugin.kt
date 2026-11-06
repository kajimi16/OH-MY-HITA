package com.stupidtree.hita.ui.credit

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hitax.data.model.timetable.TermSubject
import com.stupidtree.hitax.data.model.timetable.Timetable

class CreditUiPlugin : ComposeUiPlugin {
    override val id = "com.stupidtree.hita.ui.credit"
    override val displayName = "学分统计"
    override val requiresApi = 7

    override fun createView(context: Context, host: PluginUiHost): View {
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    val service = com.stupidtree.hita.api.TimetableService.KEY.let { host.service(it) }
                    val timetables by (service?.getTimetables() ?: androidx.lifecycle.MutableLiveData(emptyList()))
                        .observeAsState(emptyList())
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text("学分统计", style = MaterialTheme.typography.h5)
                        Text("按课表统计已导入课程的学分，不代表培养方案完成度。",
                            style = MaterialTheme.typography.caption)
                        if (timetables.isEmpty()) {
                            Text("暂无课表", modifier = Modifier.padding(top = 24.dp))
                        } else {
                            LazyColumn(Modifier.padding(top = 8.dp)) {
                                items(timetables) { timetable ->
                                    TimetableCreditCard(timetable, host)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableCreditCard(timetable: Timetable, host: PluginUiHost) {
    val service = host.service(TimetableService.KEY)
    val subjects by (service?.getSubjects(timetable.id) ?: androidx.lifecycle.MutableLiveData(emptyList<TermSubject>()))
        .observeAsState(emptyList())
    val total = subjects.sumOf { it.credit.toDouble() }.toFloat()
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(timetable.name ?: "未命名课表", style = MaterialTheme.typography.subtitle1)
            Text("课程 ${subjects.size} 门 · 学分 ${"%.1f".format(total)}")
            subjects.groupBy { it.type.name }.forEach { (type, list) ->
                Row(Modifier.fillMaxWidth()) {
                    Text(type, modifier = Modifier.weight(1f), style = MaterialTheme.typography.caption)
                    Text(
                        "${list.size} 门 / ${"%.1f".format(list.sumOf { it.credit.toDouble() }.toFloat())} 学分",
                        style = MaterialTheme.typography.caption,
                    )
                }
            }
        }
    }
}
