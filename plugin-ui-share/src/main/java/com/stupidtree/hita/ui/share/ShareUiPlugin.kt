package com.stupidtree.hita.ui.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import android.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hita.ui.share.protocol.TimetableShareCodec
import com.stupidtree.hitax.data.model.timetable.EventItem
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
                    var token by remember { mutableStateOf("") }
                    var status by remember { mutableStateOf("") }
                    val friendStore = remember { FriendStore(context) }
                    var friends by remember { mutableStateOf(friendStore.load()) }

                    fun importCode(raw: String) {
                        if (raw.isBlank()) {
                            status = "请先粘贴口令"
                            return
                        }
                        status = "正在导入…"
                        val main = android.os.Handler(android.os.Looper.getMainLooper())
                        Thread {
                            runCatching {
                                val snapshot = TimetableShareCodec().decodeMessage(raw)
                                val bundle = ShareMapper.bundle(snapshot)
                                service?.saveTimetable(bundle.timetable)
                                bundle.subjects.forEach { service?.saveSubject(it) }
                                service?.saveEvents(bundle.events)
                                snapshot
                            }.onSuccess { snapshot ->
                                val next = friendStore.remember(
                                    snapshot.shareId,
                                    snapshot.nickname,
                                    raw,
                                )
                                main.post {
                                    friends = next
                                    status = "已导入「${snapshot.term.timetableName}」"
                                }
                            }.onFailure {
                                main.post { status = "导入失败：${it.message}" }
                            }
                        }.start()
                    }

                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text("课表分享", style = MaterialTheme.typography.h5)
                        Text("HITA1 口令可导入/导出；ICS 仍可通过系统分享。",
                            style = MaterialTheme.typography.caption)

                        Column {
                            OutlinedTextField(
                                value = token,
                                onValueChange = { token = it },
                                label = { Text("粘贴 HITA1 口令") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                            )
                            Button(onClick = { importCode(token.trim()) }) { Text("导入") }
                        }
                        if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.caption)
                        if (friends.isNotEmpty()) {
                            Text("好友课表", style = MaterialTheme.typography.subtitle1,
                                modifier = Modifier.padding(top = 12.dp))
                            friends.forEach { friend ->
                                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text(friend.nickname, style = MaterialTheme.typography.subtitle2)
                                        Row {
                                            Button(onClick = { importCode(friend.code) }) { Text("再导入") }
                                            Button(
                                                onClick = { friends = friendStore.remove(friend.shareId) },
                                                modifier = Modifier.padding(start = 6.dp),
                                            ) { Text("删除") }
                                        }
                                    }
                                }
                            }
                        }
                        Text("我的课表", style = MaterialTheme.typography.subtitle1,
                            modifier = Modifier.padding(top = 12.dp))

                        LazyColumn(Modifier.padding(top = 4.dp)) {
                            items(timetables) { timetable ->
                                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(timetable.name ?: "未命名课表",
                                            style = MaterialTheme.typography.subtitle1)
                                        Text("HITA1 / ICS", style = MaterialTheme.typography.caption)
                                        Row {
                                        Button(onClick = {
                                            val query = service?.getEventsDuring(
                                                timetable.startTime.time,
                                                timetable.endTime.time,
                                            ) ?: return@Button
                                            val observer = object : androidx.lifecycle.Observer<List<EventItem>> {
                                                override fun onChanged(events: List<EventItem>) {
                                                    query.removeObserver(this)
                                                    runCatching {
                                                        val snapshot = ShareMapper.snapshot(
                                                            timetable,
                                                            events,
                                                            "HITA 用户",
                                                        )
                                                        TimetableShareCodec().encode(snapshot)
                                                    }.onSuccess { code ->
                                                        showToken(context, code)
                                                    }.onFailure {
                                                        Toast.makeText(context, "生成失败：${it.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                            query.observeForever(observer)
                                        }) { Text("口令") }
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
                                                            Intent.createChooser(intent, "分享课表")
                                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        )
                                                    }.onFailure {
                                                        Toast.makeText(context, "分享失败：${it.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                            export.observeForever(observer)
                                        }) { Text("ICS") }
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

    private fun showToken(context: Context, token: String) {
        AlertDialog.Builder(context)
            .setTitle("HITA1 口令")
            .setMessage(token)
            .setPositiveButton("复制") { _, _ ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("HITA1", token))
                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("分享") { _, _ ->
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, token)
                        },
                        "分享口令",
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
