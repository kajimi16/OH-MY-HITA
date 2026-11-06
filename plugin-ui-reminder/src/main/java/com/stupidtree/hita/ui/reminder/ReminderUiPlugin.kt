package com.stupidtree.hita.ui.reminder

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.stupidtree.hita.api.BackgroundTask
import com.stupidtree.hita.api.BackgroundTaskHost
import com.stupidtree.hita.api.BackgroundTaskService
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.ReminderService
import com.stupidtree.hita.api.ReminderSpec
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hita.api.WidgetContent
import com.stupidtree.hita.api.WidgetContentProvider
import com.stupidtree.hita.api.WidgetHost
import com.stupidtree.hita.api.WidgetOptions
import com.stupidtree.hita.api.WidgetRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderUiPlugin : ComposeUiPlugin, WidgetContentProvider, BackgroundTask {

    override val id = "com.stupidtree.hita.ui.reminder"
    override val displayName = "上课提醒"
    override val requiresApi = 7

    override fun run(context: Context, host: BackgroundTaskHost): Boolean {
        val prefs = context.getSharedPreferences("reminder_plugin", Context.MODE_PRIVATE)
        val count = prefs.getInt("background_runs", 0) + 1
        prefs.edit().putInt("background_runs", count).apply()
        return true
    }

    override fun buildWidget(
        context: Context,
        host: WidgetHost,
        options: WidgetOptions,
    ): WidgetContent {
        val timetable = host.service(TimetableService.KEY)
        val events = runCatching { timetable?.getTodayEventsSync().orEmpty() }.getOrDefault(emptyList())
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return WidgetContent(
            title = "今日课程",
            subtitle = if (events.isEmpty()) "今天没有课程" else "共 ${events.size} 节",
            rows = events.take(6).map { event ->
                WidgetRow(
                    leading = formatter.format(Date(event.from.time)),
                    title = event.name,
                    trailing = event.place.orEmpty(),
                )
            },
        )
    }

    override fun createView(context: Context, host: PluginUiHost): View {
        val prefs = context.getSharedPreferences("reminder_plugin", Context.MODE_PRIVATE)
        val ownerId = id
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    val reminders = remember { host.service(ReminderService.KEY) }
                    val timetable = remember { host.service(TimetableService.KEY) }
                    var enabled by remember { mutableStateOf(prefs.getBoolean("enabled", false)) }
                    var minutes by remember { mutableStateOf(prefs.getInt("minutes", 15).toFloat()) }
                    var status by remember { mutableStateOf("") }
                    var permission by remember { mutableStateOf(hasNotificationPermission(context)) }
                    var backgroundRuns by remember {
                        mutableStateOf(prefs.getInt("background_runs", 0))
                    }

                    fun reschedule() {
                        val reminderService = reminders ?: run {
                            status = "宿主未提供提醒服务"
                            return
                        }
                        if (!reminderService.canPostNotifications()) {
                            status = "请先授予通知权限"
                            return
                        }
                        val events = runCatching { timetable?.getTodayEventsSync().orEmpty() }
                            .getOrDefault(emptyList())
                        val now = System.currentTimeMillis()
                        reminderService.cancelAll(ownerId)
                        var count = 0
                        events.forEach { event ->
                            val trigger = event.from.time - (minutes.toInt() * 60_000L)
                            if (trigger > now) {
                                val ok = reminderService.schedule(
                                    ReminderSpec(
                                        ownerId = ownerId,
                                        id = event.id,
                                        triggerAtMillis = trigger,
                                        title = "即将上课",
                                        text = "${event.name} @ ${event.place.orEmpty()} · ${minutes.toInt()} 分钟后",
                                    )
                                )
                                if (ok) count++
                            }
                        }
                        status = if (count == 0) "今天没有可安排的课程" else "已安排 $count 条提醒"
                    }

                    LaunchedEffect(enabled, minutes) {
                        if (enabled) withContext(Dispatchers.IO) { reschedule() }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text("上课提醒", style = MaterialTheme.typography.h5)
                        Text(
                            "由宿主统一调度；关闭或卸载插件不会删除已安排的提醒，请先关闭开关。",
                            style = MaterialTheme.typography.body2,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("开启提醒", modifier = Modifier.weight(1f))
                            Switch(
                                checked = enabled,
                                onCheckedChange = { checked ->
                                    if (checked && !hasNotificationPermission(context)) {
                                        (context as? Activity)?.requestPermissions(
                                            arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001
                                        )
                                        status = "授予通知权限后再开启"
                                        return@Switch
                                    }
                                    enabled = checked
                                    prefs.edit().putBoolean("enabled", checked).apply()
                                    if (!checked) {
                                        reminders?.cancelAll(ownerId)
                                        status = "已关闭并取消提醒"
                                    }
                                },
                            )
                        }
                        Text("提前 ${minutes.toInt()} 分钟")
                        Slider(
                            value = minutes,
                            onValueChange = { minutes = it },
                            valueRange = 1f..60f,
                            steps = 58,
                        )
                        Button(onClick = {
                            if (enabled) {
                                prefs.edit().putInt("minutes", minutes.toInt()).apply()
                                reschedule()
                            }
                        }) { Text("保存并重排") }
                        Button(onClick = {
                            val reminderService = reminders
                            if (reminderService == null) {
                                status = "宿主未提供提醒服务"
                                return@Button
                            }
                            if (!reminderService.canPostNotifications()) {
                                status = "请先授予通知权限"
                                return@Button
                            }
                            val ok = reminderService.schedule(
                                ReminderSpec(
                                    ownerId = ownerId,
                                    id = "test-${System.currentTimeMillis()}",
                                    triggerAtMillis = System.currentTimeMillis() + 10_000,
                                    title = "测试提醒",
                                    text = "10 秒后送达",
                                )
                            )
                            status = if (ok) "测试提醒已安排，10 秒后送达" else "测试提醒安排失败"
                        }) { Text("10 秒后测试") }
                        Text("后台任务执行：$backgroundRuns 次")
                        Button(onClick = {
                            val service = host.service(BackgroundTaskService.KEY)
                            if (service == null) {
                                status = "宿主未提供后台任务服务"
                            } else {
                                val ok = service.scheduleOnce(ownerId, "test-once", 5_000)
                                status = if (ok) "5 秒后台任务已注册" else "后台任务注册失败"
                            }
                        }) { Text("5 秒后台任务") }
                        Button(onClick = {
                            backgroundRuns = prefs.getInt("background_runs", 0)
                        }) { Text("刷新执行次数") }
                        Text(status.ifBlank { "通知权限：${if (permission) "已授予" else "未授予"}" })
                        Text(
                            "提前量会保存在插件设置中；当前只安排今天的课程。",
                            style = MaterialTheme.typography.caption,
                        )
                    }
                }
            }
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
        return true
    }
}
