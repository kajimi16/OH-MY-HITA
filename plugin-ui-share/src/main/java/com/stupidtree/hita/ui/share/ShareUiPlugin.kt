package com.stupidtree.hita.ui.share

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hitax.utils.FileProviderUtils
import com.stupidtree.hitax.utils.ShareUtils
import java.io.File

class ShareUiPlugin : ComposeUiPlugin {
    override val id = "com.stupidtree.hita.ui.share"
    override val displayName = "课表分享"
    override val requiresApi = 7

    override fun createView(context: Context, host: PluginUiHost): View {
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    val service = host.service(TimetableService.KEY)
                    val timetables by (service?.getTimetables()
                        ?: androidx.lifecycle.MutableLiveData(emptyList()))
                        .observeAsState(emptyList())
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text("课表分享", style = MaterialTheme.typography.h5)
                        Text("导出为 ICS 后通过系统分享发送。HITA1 口令暂未支持。",
                            style = MaterialTheme.typography.caption)
                        if (timetables.isEmpty()) {
                            Text("暂无课表", modifier = Modifier.padding(top = 24.dp))
                        } else {
                            LazyColumn(Modifier.padding(top = 8.dp)) {
                                items(timetables) { timetable ->
                                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Row(Modifier.padding(12.dp)) {
                                            Column(Modifier.weight(1f)) {
                                                Text(timetable.name ?: "未命名课表",
                                                    style = MaterialTheme.typography.subtitle1)
                                                Text("ICS 日历", style = MaterialTheme.typography.caption)
                                            }
                                            Button(onClick = {
                                                val export = service?.exportToICS(
                                                    timetable.name ?: "课表",
                                                    timetable.id,
                                                ) ?: return@Button
                                                val observer = object : androidx.lifecycle.Observer<DataState<String>> {
                                                    override fun onChanged(result: DataState<String>) {
                                                        if (result.state != DataState.STATE.SUCCESS) return
                                                        export.removeObserver(this)
                                                        val file = result.data?.let { File(it) }
                                                        if (file == null || !file.isFile) {
                                                            Toast.makeText(context, "导出文件不存在", Toast.LENGTH_SHORT).show()
                                                            return
                                                        }
                                                        runCatching {
                                                            val uri = FileProviderUtils.getUriForFile(context, file)
                                                            val intent = ShareUtils.buildShareIntentForStream(uri, "text/calendar")
                                                            context.startActivity(
                                                                Intent.createChooser(intent, "分享课表").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            )
                                                        }.onFailure {
                                                            Toast.makeText(context, "分享失败：${it.message}", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                }
                                                export.observeForever(observer)
                                            }) { Text("分享") }
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
}
