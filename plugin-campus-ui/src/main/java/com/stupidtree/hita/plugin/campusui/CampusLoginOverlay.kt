package com.stupidtree.hita.plugin.campusui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.stupidtree.hita.api.CampusService
import com.stupidtree.hita.api.CampusServiceTarget
import kotlinx.coroutines.delay

/**
 * 门户登录覆盖层：在 WebView 里完成网页登录，Cookie 由 WebView 自己保存。
 *
 * 契约：页面每次加载完成后每秒轮询一次 [CampusService.session]（只读 Cookie），
 * 检测到该门户会话即回调 [onSignedIn]；用户始终可以关闭、后退或刷新。
 * 只允许 http/https 导航，其它 scheme 直接拦截。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun CampusLoginOverlay(
    service: CampusService,
    target: CampusServiceTarget,
    onClose: () -> Unit,
    onSignedIn: () -> Unit,
) {
    val context = LocalContext.current
    var progress by remember(target) { mutableIntStateOf(0) }
    var pageLoaded by remember(target) { mutableStateOf(false) }
    var pageTitle by remember(target) { mutableStateOf("") }
    var canGoBack by remember(target) { mutableStateOf(false) }
    var hint by remember(target) { mutableStateOf<String?>(null) }

    val webView = remember(target) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    val scheme = request.url.scheme.orEmpty().lowercase()
                    return scheme != "http" && scheme != "https"
                }

                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
                    val scheme = url?.substringBefore("://")?.lowercase().orEmpty()
                    return scheme != "http" && scheme != "https"
                }

                override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                    pageLoaded = false
                    progress = 0
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    pageTitle = view.title.orEmpty()
                    canGoBack = view.canGoBack()
                    progress = 100
                    pageLoaded = true
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progress = newProgress
                    canGoBack = view.canGoBack()
                }
            }
            loadUrl(portalUrl(target))
        }
    }

    LaunchedEffect(webView) {
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
    }

    DisposableEffect(webView) {
        onDispose {
            runCatching {
                webView.stopLoading()
                webView.destroy()
            }
        }
    }

    // 页面每次加载完成后开始轮询会话；Cookie 写入是异步的，先 flush 再读。
    LaunchedEffect(pageLoaded, target) {
        if (!pageLoaded) return@LaunchedEffect
        while (true) {
            delay(1_000)
            val signedIn = runCatching {
                CookieManager.getInstance().flush()
                sessionSignedIn(service.session(), target)
            }.getOrDefault(false)
            if (signedIn) {
                onSignedIn()
                break
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "登录${targetLabel(target)}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClose) { Text("关闭") }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                TextButton(
                    enabled = canGoBack,
                    onClick = { if (webView.canGoBack()) webView.goBack() },
                ) { Text("后退") }
                TextButton(onClick = { webView.reload() }) { Text("刷新") }
                TextButton(
                    onClick = {
                        val signedIn = runCatching {
                            CookieManager.getInstance().flush()
                            sessionSignedIn(service.session(), target)
                        }.getOrDefault(false)
                        if (signedIn) {
                            onSignedIn()
                        } else {
                            hint = "仍未检测到登录，请完成网页登录后再试。"
                        }
                    },
                ) { Text("检查登录") }
            }
            if (progress in 0..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = hint ?: pageTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            )
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }
}

