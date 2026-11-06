package com.stupidtree.hita.ui.notice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.viewinterop.AndroidView
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class NoticeUiPlugin : ComposeUiPlugin {
    override val id = "com.stupidtree.hita.ui.notice"
    override val displayName = "校园通知"
    override val requiresApi = 7

    override fun createView(context: Context, host: PluginUiHost): View {
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    var cookie by remember { mutableStateOf(readCookie()) }
                    var showLogin by remember { mutableStateOf(cookie.isBlank()) }
                    var notices by remember { mutableStateOf<List<CampusNotice>>(emptyList()) }
                    var loading by remember { mutableStateOf(false) }
                    var error by remember { mutableStateOf<String?>(null) }
                    var reload by remember { mutableStateOf(0) }

                    LaunchedEffect(reload, cookie) {
                        if (cookie.isBlank()) return@LaunchedEffect
                        loading = true
                        error = null
                        runCatching { withContext(Dispatchers.IO) { fetch(cookie) } }
                            .onSuccess { notices = it }
                            .onFailure { error = it.message ?: "加载失败" }
                        loading = false
                    }

                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Row {
                            Text("校园通知", style = MaterialTheme.typography.h5,
                                modifier = Modifier.padding(end = 8.dp))
                            Button(onClick = {
                                cookie = readCookie()
                                if (cookie.isBlank()) error = "尚未登录"
                                else { showLogin = false; reload++ }
                            }) { Text("我已完成登录") }
                            Button(onClick = { showLogin = !showLogin }) {
                                Text(if (showLogin) "隐藏登录" else "登录门户")
                            }
                        }
                        Text(if (cookie.isBlank()) "请先在下方登录 info.hitsz.edu.cn"
                        else "已读取门户 Cookie", style = MaterialTheme.typography.caption)

                        if (showLogin) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        webViewClient = WebViewClient()
                                        CookieManager.getInstance().setAcceptCookie(true)
                                        loadUrl(NoticeParser.PORTAL)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            )
                        } else {
                            when {
                                loading -> CircularProgressIndicator(Modifier.padding(top = 24.dp))
                                error != null -> Text("加载失败：$error")
                                notices.isEmpty() -> Text("暂无通知")
                                else -> LazyColumn(Modifier.padding(top = 8.dp)) {
                                    items(notices) { notice ->
                                        Card(
                                            Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                                .clickable {
                                                    runCatching {
                                                        context.startActivity(
                                                            Intent(Intent.ACTION_VIEW, Uri.parse(notice.url))
                                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        )
                                                    }
                                                },
                                        ) {
                                            Column(Modifier.padding(12.dp)) {
                                                Text(notice.title, style = MaterialTheme.typography.subtitle1)
                                                Text(notice.date, style = MaterialTheme.typography.caption)
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

    private fun readCookie(): String = runCatching {
        CookieManager.getInstance().getCookie(NoticeParser.PORTAL).orEmpty()
    }.getOrDefault("")

    private fun fetch(cookie: String): List<CampusNotice> {
        val result = LinkedHashMap<String, CampusNotice>()
        for (page in 1..3) {
            val url = NoticeParser.listUrl(page)
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 20_000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("Cookie", cookie)
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 OH-MY-HITA")
            val html = try {
                if (NoticeParser.isLogin(conn.url.toString()) || conn.responseCode in 401..403) {
                    error("门户会话已失效，请重新登录")
                }
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
            val parsed = NoticeParser.parse(html, url)
            if (parsed.isEmpty()) break
            parsed.forEach { result.putIfAbsent(it.id, it) }
            if (result.size >= 30) break
        }
        return result.values.sortedByDescending { it.time }.take(30)
    }
}
