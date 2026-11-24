package com.stupidtree.hita.plugin.todoui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stupidtree.hita.api.TodoService
import kotlinx.coroutines.launch

/** Grader CAS 入口页；登录成功后 CookieManager 会写入 sessionid。 */
internal const val GRADER_ORIGIN = "https://grader.hitsz.edu.cn"

/**
 * 内嵌 WebView 完成 Grader 的学校 CAS 登录。
 *
 * 前置条件：宿主具备 INTERNET 权限；[service] 可用。
 * 后置条件：每次主页面加载完成都读取 [GRADER_ORIGIN] 的 Cookie；出现非空 sessionid 时调用
 * [TodoService.connectGrader]，成功后回调 [onSuccess] 并关闭本对话框。失败显示原因并允许重试。
 * 不变式：sessionid 只存在于内存中的 Cookie 头，不写入界面状态或日志；连接成功后不再提交。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun GraderWebLoginDialog(
    service: TodoService,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var connecting by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val webView = remember { mutableStateOf<WebView?>(null) }
    val scope = rememberCoroutineScope()
    val currentOnSuccess by rememberUpdatedState(onSuccess)

    fun connect(header: String) {
        if (connecting || done) return
        connecting = true
        error = null
        scope.launch {
            val ok = try {
                service.connectGrader(header)
            } finally {
                connecting = false
            }
            if (ok) {
                done = true
                currentOnSuccess()
            } else {
                error = "已检测到 sessionid，但连接失败。请确认 CAS 已登录，或返回后改用粘贴 Cookie。"
            }
        }
    }

    fun checkCookie() {
        if (connecting || done) return
        val header = CookieManager.getInstance().getCookie(GRADER_ORIGIN).orEmpty()
        if (hasGraderSession(header)) connect(header)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Grader 网页登录", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (connecting) "已获取会话，正在连接…"
                            else "在下方完成学校 CAS 登录，成功后自动连接。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        onClick = { webView.value?.goBack() },
                        enabled = canGoBack && !connecting,
                    ) { Text("后退") }
                    TextButton(
                        onClick = { webView.value?.reload() },
                        enabled = !connecting,
                    ) { Text("刷新") }
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
                if (loading || connecting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    Spacer(Modifier.height(4.dp))
                }
                error?.let {
                    Text(
                        it,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    factory = { context ->
                        WebView(context).apply {
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                                    loading = true
                                    error = null
                                }

                                override fun onPageFinished(view: WebView, url: String) {
                                    loading = false
                                    canGoBack = view.canGoBack()
                                    checkCookie()
                                }

                                override fun onReceivedError(
                                    view: WebView,
                                    request: WebResourceRequest,
                                    webError: WebResourceError,
                                ) {
                                    if (!request.isForMainFrame) return
                                    loading = false
                                    error = "页面加载失败：${webError.description}。请检查网络后点刷新。"
                                }
                            }
                            webView.value = this
                            loadUrl("$GRADER_ORIGIN/api/login/sso")
                        }
                    },
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.value?.let { view ->
                (view.parent as? ViewGroup)?.removeView(view)
                view.stopLoading()
                view.destroy()
            }
        }
    }
}

/**
 * 判断 Cookie 头是否包含非空 sessionid。
 *
 * 前置条件：无。
 * 后置条件：存在形如 `sessionid=<非空>` 的片段时返回 true；重复项只要有一项非空即返回 true。
 * 不变式：纯函数，不修改入参。
 */
internal fun hasGraderSession(header: String): Boolean =
    header.split(';').any {
        val segment = it.trim()
        segment.substringBefore('=').trim() == "sessionid" && segment.substringAfter('=', "").isNotBlank()
    }
