package com.stupidtree.hita.ui.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.google.gson.JsonParser
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class UpdateUiPlugin : ComposeUiPlugin {
    override val id = "com.stupidtree.hita.ui.update"
    override val displayName = "应用更新"
    override val requiresApi = 7

    override fun createView(context: Context, host: PluginUiHost): View {
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    var latest by remember { mutableStateOf<String?>(null) }
                    var body by remember { mutableStateOf("") }
                    var error by remember { mutableStateOf<String?>(null) }
                    var current by remember { mutableStateOf("") }
                    LaunchedEffect(Unit) {
                        current = runCatching {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
                        }.getOrDefault("")
                        runCatching { withContext(Dispatchers.IO) { fetchLatest() } }
                            .onSuccess { latest = it.first; body = it.second }
                            .onFailure { error = it.message ?: "检查失败" }
                    }
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text("应用更新", style = MaterialTheme.typography.h5)
                        Text("当前版本：$current", style = MaterialTheme.typography.body2)
                        when {
                            error != null -> Text("检查失败：$error")
                            latest == null -> Text("检查中…")
                            else -> {
                                Text("最新版本：$latest", style = MaterialTheme.typography.subtitle1)
                                Text(body.take(1000), style = MaterialTheme.typography.caption)
                                Button(onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }) { Text("打开下载页") }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fetchLatest(): Pair<String, String> {
        val conn = URL(API).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "OH-MY-HITA-Update")
        return try {
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val obj = JsonParser.parseString(text).asJsonObject
            (obj.get("tag_name")?.asString ?: "unknown") to
                (obj.get("body")?.asString ?: "")
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val API = "https://api.github.com/repos/kajimi16/OH-MY-HITA/releases/latest"
        const val RELEASES = "https://github.com/kajimi16/OH-MY-HITA/releases/latest"
    }
}
