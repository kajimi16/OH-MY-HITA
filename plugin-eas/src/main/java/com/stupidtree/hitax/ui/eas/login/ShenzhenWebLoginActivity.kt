package com.stupidtree.hitax.ui.eas.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stupidtree.hitax.data.source.preference.EasPreferenceSource

/**
 * 深圳教务 Web 会话登录。
 *
 * 培养方案/部分接口依赖 jw.hitsz.edu.cn 的 Web session，而 EAS 账号登录只拿到
 * API token。这里让用户完成一次 WebView 登录，成功后把 JSESSIONID/route/SESSION
 * 合并进现有 EASToken.cookies。
 */
class ShenzhenWebLoginActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var status: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var finished = false
    private var attempts = 0
    private var lastAdvancedUrl: String? = null
    private var validating = false

    private val pollCookies = object : Runnable {
        override fun run() {
            if (finished) return
            val cookies = collectCookies()
            val url = webView.url.orEmpty()
            if (ShenzhenWebLoginPolicy.isAuthenticatedPage(url, cookies) && !validating) {
                validateAndSave(cookies)
                return
            }
            if (lastAdvancedUrl != url) {
                lastAdvancedUrl = url
                webView.evaluateJavascript(ShenzhenWebLoginPolicy.clickScript(), null)
            }
            attempts++
            if (attempts >= MAX_ATTEMPTS) {
                status.text = "仍未检测到教务 Web 会话，可返回后重试"
                return
            }
            handler.postDelayed(this, POLL_MS)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        status = TextView(this).apply {
            text = "正在打开深圳教务…"
            textSize = 14f
            setPadding(32, 24, 32, 12)
        }
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.userAgentString = DESKTOP_USER_AGENT
            setBackgroundColor(android.graphics.Color.WHITE)
            setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    status.text = "请在页面完成登录，成功后会自动返回"
                    lastAdvancedUrl = null
                    handler.removeCallbacks(pollCookies)
                    handler.postDelayed(pollCookies, 600)
                }
            }
            webChromeClient = WebChromeClient()
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        clearOldSessionCookies()
        root.addView(
            status,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        root.addView(
            webView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ).apply { gravity = Gravity.FILL },
        )
        setContentView(root)
        webView.loadUrl(PROXY_BASE)
        handler.postDelayed(pollCookies, 1_000)
    }

    private fun validateAndSave(cookies: Map<String, String>) {
        validating = true
        status.text = "正在验证教务 Web 会话…"
        Thread {
            var merged: Map<String, String> = cookies
            val valid = runCatching {
                val response = org.jsoup.Jsoup.connect(PROXY_BASE + "UserManager/queryxsxx")
                    .cookies(cookies)
                    .header("Referer", PROXY_BASE + "authentication/main")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .userAgent(DESKTOP_USER_AGENT)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .timeout(12_000)
                    .method(org.jsoup.Connection.Method.POST)
                    .execute()
                val body = response.body()
                val rotated = LinkedHashMap(cookies)
                rotated.putAll(response.cookies())
                merged = rotated
                response.statusCode() == 200 &&
                    !body.trimStart().startsWith("<!DOCTYPE", ignoreCase = true) &&
                    !body.contains("trust.hitsz.edu.cn") &&
                    body.contains("{")
            }.getOrDefault(false)
            runOnUiThread {
                validating = false
                if (valid) {
                    saveCookies(merged)
                } else {
                    status.text = "会话尚未生效，请在页面完成登录"
                    handler.postDelayed(pollCookies, POLL_MS)
                }
            }
        }.start()
    }

    private fun clearOldSessionCookies() {
        val manager = CookieManager.getInstance()
        listOf(DIRECT_BASE, PROXY_BASE).forEach { base ->
            listOf("SESSION", "JSESSIONID", "route").forEach { name ->
                manager.setCookie(base, "$name=; Max-Age=0; Path=/")
            }
        }
        manager.flush()
    }

    private fun collectCookies(): MutableMap<String, String> {
        val manager = CookieManager.getInstance()
        val result = linkedMapOf<String, String>()
        listOf(DIRECT_BASE, PROXY_BASE).forEach { base ->
            manager.getCookie(base).orEmpty().split(";").forEach { part ->
                val text = part.trim()
                val index = text.indexOf('=')
                if (index > 0) {
                    result[text.substring(0, index).trim()] = text.substring(index + 1).trim()
                }
            }
        }
        return result
    }

    private fun hasSession(cookies: Map<String, String>): Boolean {
        val direct = !cookies["JSESSIONID"].isNullOrBlank() && !cookies["route"].isNullOrBlank()
        val proxy = !cookies["SESSION"].isNullOrBlank()
        return direct || proxy
    }

    private fun saveCookies(cookies: Map<String, String>) {
        if (finished) return
        finished = true
        handler.removeCallbacks(pollCookies)
        runCatching {
            val prefs = EasPreferenceSource.getInstance(applicationContext)
            val token = prefs.getEasToken()
            token.webCookies.clear()
            listOf("SESSION", "JSESSIONID", "route").forEach { token.cookies.remove(it) }
            token.webCookies.putAll(cookies)
            val host = android.net.Uri.parse(webView.url.orEmpty()).host.orEmpty()
            token.webBaseUrl = if (host.contains("jw-hitsz-edu-cn")) PROXY_BASE.trimEnd('/') else DIRECT_BASE.trimEnd('/')
            prefs.saveEasToken(token)
            CookieManager.getInstance().flush()
        }.onSuccess {
            Toast.makeText(this, "已连接深圳教务 Web 会话", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }.onFailure {
            status.text = "保存会话失败：${it.message}"
            finished = false
        }
    }

    override fun onDestroy() {
        finished = true
        handler.removeCallbacks(pollCookies)
        webView.destroy()
        super.onDestroy()
    }

    private companion object {
        const val DIRECT_BASE = "https://jw.hitsz.edu.cn/"
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"
        const val PROXY_BASE = "https://jw-hitsz-edu-cn.hitsz.edu.cn/"
        const val POLL_MS = 800L
        const val MAX_ATTEMPTS = 150
    }
}
