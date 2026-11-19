package com.stupidtree.hita.ui.announcement

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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** 版本/服务公告。首版以 GitHub Releases 作为公告源。 */
class AnnouncementUiPlugin : ComposeUiPlugin {
    override val id = "com.stupidtree.hita.ui.announcement"
    override val displayName = "公告"
    override val requiresApi = 7

    override fun createView(context: Context, host: PluginUiHost): View {
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    var reload by remember { mutableIntStateOf(0) }
                    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
                    var error by remember { mutableStateOf<String?>(null) }
                    var loading by remember { mutableStateOf(true) }
                    LaunchedEffect(reload) {
                        loading = true
                        error = null
                        runCatching { withContext(Dispatchers.IO) { fetch(context) } }
                            .onSuccess { announcements = listOf(localAnnouncement()) + it }
                            .onFailure { error = it.message ?: "加载失败" }
                        loading = false
                    }
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { reload++ }) { Text("刷新") }
                        }
                        when {
                            loading -> CircularProgressIndicator(Modifier.padding(24.dp))
                            error != null -> Column(Modifier.padding(24.dp)) {
                                Text("加载失败：$error")
                                TextButton(onClick = { reload++ }) { Text("重试") }
                            }
                            announcements.isEmpty() -> Text("暂无公告", Modifier.padding(24.dp))
                            else -> LazyColumn(Modifier.fillMaxSize()) {
                                items(announcements) { item ->
                                    AnnouncementCard(item)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fetch(context: Context): List<Announcement> {
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.1"
        }.getOrDefault("0.0.1")
        val url = "$API?app_id=${URLEncoder.encode("oh-my-hita", "UTF-8")}" +
            "&app_version=${URLEncoder.encode(version, "UTF-8")}&build=1"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "OH-MY-HITA/$version")
        return try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            val root = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val array = root.optJSONArray("notices") ?: JSONArray()
            (0 until array.length()).mapNotNull { index ->
                val obj = array.optJSONObject(index) ?: return@mapNotNull null
                Announcement(
                    title = obj.optString("title", "公告"),
                    kind = obj.optString("kind", "service"),
                    body = obj.optString("body"),
                )
            }
        } finally {
            conn.disconnect()
        }
    }

    data class Announcement(
        val title: String,
        val kind: String,
        val body: String,
    )

    private fun localAnnouncement() = Announcement(
        title = "OH-MY-HITA 开发中",
        kind = "本地公告",
        body = "oh-my-hita开发中，欢迎开发插件，" +
            "如果有需要拓展的宿主能力请直接邮件联系开发者卡基米1902163454@qq.com",
    )

    companion object {
        private const val API = "https://api.hita.limpu.cn/api/notices"
    }
}

@Composable
private fun AnnouncementCard(item: AnnouncementUiPlugin.Announcement) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                item.kind,
                fontSize = 12.sp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 4.dp),
            )
            if (item.body.isNotBlank()) {
                Text(
                    item.body.take(1000),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
