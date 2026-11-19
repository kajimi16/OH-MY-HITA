package com.stupidtree.hitax.shell.news

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

/** 原版资讯页的校园门户登录入口；登录完成后返回资讯页刷新通知。 */
class ShellPortalLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CookieManager.getInstance().setAcceptCookie(true)
        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            loadUrl(NoticeParser.PORTAL)
        }
        setContentView(webView)
    }
}
