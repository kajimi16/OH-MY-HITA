package com.stupidtree.hitax.shell.news

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

/** 资讯/通知的站内阅读页；复用 WebView Cookie，避免跳到外部浏览器。 */
class ShellNewsDetailActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_URL = "url"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (url.isBlank()) {
            finish()
            return
        }
        CookieManager.getInstance().setAcceptCookie(true)
        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            webViewClient = WebViewClient()
            loadUrl(url)
        }
        setContentView(webView)
    }
}
