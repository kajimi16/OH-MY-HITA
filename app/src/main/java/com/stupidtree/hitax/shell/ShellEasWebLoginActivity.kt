package com.stupidtree.hitax.shell

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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stupidtree.hita.api.EasAuthService
import com.stupidtree.hitax.HApplication
import org.json.JSONObject

/**
 * Host T3 WebView login for Shenzhen academic web session.
 *
 * The runtime plugin stays free of Activities/resources; this screen only collects
 * cookies and hands them to [EasAuthService.loginWithWebSession].
 */
class ShellEasWebLoginActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var status: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var finished = false
    private var validating = false
    private var attempts = 0
    private var lastUrl: String? = null

    private val auth: EasAuthService?
        get() = (application as? HApplication)?.pluginHost?.service(EasAuthService.KEY)

    private val poll = object : Runnable {
        override fun run() {
            if (finished) return
            val cookies = collectCookies()
            val url = webView.url.orEmpty()
            android.util.Log.d("ShellEasWeb", "url=$url cookies=${cookies.keys}")
            if (isAuthenticated(url, cookies) && !validating) {
                submit(cookies)
                return
            }
            if (lastUrl != url) {
                lastUrl = url
                webView.evaluateJavascript(CLICK_SCRIPT, null)
            }
            if (++attempts >= MAX_ATTEMPTS) {
                status.text = "仍未检测到教务 Web 会话，可返回后重试"
                return
            }
            handler.postDelayed(this, POLL_MS)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        status = TextView(this).apply {
            text = "正在打开深圳教务…"
            textSize = 14f
            setPadding(48, 32, 48, 16)
        }
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = false
            settings.useWideViewPort = false
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.userAgentString = DESKTOP_UA
            setBackgroundColor(android.graphics.Color.WHITE)
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    status.text = "请在页面完成登录，成功后会自动返回"
                    lastUrl = null
                    attempts = 0
                    view?.evaluateJavascript("document.body.style.zoom='0.65';", null)
                    handler.removeCallbacks(poll)
                    handler.postDelayed(poll, 600)
                }
            }
            webChromeClient = WebChromeClient()
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        val loginButton = Button(this).apply {
            text = "统一身份认证登录"
            setOnClickListener {
                webView.evaluateJavascript(CLICK_SCRIPT, null)
            }
        }
        root.addView(
            status,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        root.addView(
            loginButton,
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
        handler.postDelayed(poll, 1_000)
    }

    private fun submit(cookies: Map<String, String>) {
        validating = true
        status.text = "正在验证教务 Web 会话…"
        val base = if (webView.url.orEmpty().contains("jw-hitsz-edu-cn")) {
            PROXY_BASE.trimEnd('/')
        } else {
            DIRECT_BASE.trimEnd('/')
        }
        val service = auth
        if (service == null) {
            Toast.makeText(this, "教务插件未安装", Toast.LENGTH_SHORT).show()
            validating = false
            return
        }
        val result = runCatching { service.loginWithWebSession(cookies, base) }.getOrNull()
        if (result == null) {
            status.text = "当前教务插件不支持 Web 登录"
            Toast.makeText(this, "请更新教务插件", Toast.LENGTH_SHORT).show()
            validating = false
            return
        }
        result.observeForever { state ->
            if (state.state == com.stupidtree.component.data.DataState.STATE.SUCCESS && state.data == true) {
                finished = true
                CookieManager.getInstance().flush()
                setResult(RESULT_OK)
                finish()
            } else {
                status.text = state.message ?: "Web 会话验证失败"
                Toast.makeText(this, status.text, Toast.LENGTH_SHORT).show()
                validating = false
                handler.postDelayed(poll, POLL_MS)
            }
        }
    }

    private fun isAuthenticated(url: String, cookies: Map<String, String>): Boolean {
        val path = android.net.Uri.parse(url).path.orEmpty().lowercase()
        if (path.contains("login") || path.contains("authserver") ||
            path.contains("authentication/require") || path.contains("trust")
        ) return false
        val academic = path.contains("authentication/main") || path.contains("student_index") ||
            path.contains("user/me") || path.contains("xszykb") || path.contains("xsxk")
        val session = !cookies["SESSION"].isNullOrBlank() ||
            (!cookies["JSESSIONID"].isNullOrBlank() && !cookies["route"].isNullOrBlank())
        return academic && session
    }

    private fun collectCookies(): Map<String, String> {
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

    override fun onDestroy() {
        finished = true
        handler.removeCallbacks(poll)
        webView.destroy()
        super.onDestroy()
    }

    private companion object {
        const val DIRECT_BASE = "https://jw.hitsz.edu.cn/"
        const val PROXY_BASE = "https://jw-hitsz-edu-cn.hitsz.edu.cn/"
        const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"
        const val POLL_MS = 800L
        const val MAX_ATTEMPTS = 150
        val CLICK_SCRIPT = """
            (function(){
              function n(v){return String(v||'').replace(/\s+/g,'');}
              function visible(e){if(!e||typeof e.click!=='function')return false;var s=getComputedStyle?getComputedStyle(e):null;if(s&&(s.display==='none'||s.visibility==='hidden'))return false;var r=e.getBoundingClientRect?e.getBoundingClientRect():null;return !r||(r.width>0&&r.height>0);}
              function label(e){return n(e.innerText||e.textContent||e.value||e.getAttribute('aria-label')||e.getAttribute('title')||'');}
              var items=Array.prototype.slice.call(document.querySelectorAll('button,a,[role="button"],input[type="button"],input[type="submit"],[onclick],[tabindex],div,span,p')).filter(visible);
              function pick(p){return items.filter(function(e){return p(label(e));}).sort(function(a,b){return label(a).length-label(b).length;})[0]||null;}
              function clickable(e){var n=e;for(var i=0;i<5&&n;i++){if(n.click&&(n.tagName==='A'||n.tagName==='BUTTON'||n.getAttribute('onclick')||n.getAttribute('role')==='button'))return n;n=n.parentElement;}return e;}
              var e=pick(function(l){return l.indexOf('统一身份认证')>=0;});
              if(e){clickable(e).click();return 'unified';}
              var page=n(document.body&&document.body.innerText);
              if(page.indexOf('本科')>=0&&page.indexOf('研究生')>=0){
                var r=pick(function(l){return l.indexOf('本科')>=0&&(l.length<=5||l.indexOf('登录')>=0||l.indexOf('入口')>=0||l.indexOf('教务')>=0);});
                if(r){r.click();return 'role';}
              }
              return 'none';
            })();
        """.trimIndent()
    }
}
