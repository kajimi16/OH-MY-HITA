package com.stupidtree.hitax.legacy.lite.ui.news

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.stupidtree.hitax.legacy.lite.databinding.LiteNewsActivityDetailBinding
import com.stupidtree.style.base.BaseActivity

/**
 * HITA_Lite 新闻详情页。
 *
 * 页面结构与 Lite 的 activity_news_detail 一致；正文直接由 WebView 加载文章地址。
 * TODO 宿主尚未提供新闻元信息契约（发布时间/正文），metaData 的 repository 拉取已移除。
 */
class LiteNewsDetailActivity :
    BaseActivity<LiteNewsDetailViewModel, LiteNewsActivityDetailBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setToolbarActionBack(binding.toolbar)
    }

    override fun initViewBinding(): LiteNewsActivityDetailBinding =
        LiteNewsActivityDetailBinding.inflate(layoutInflater)

    override fun getViewModelClass(): Class<LiteNewsDetailViewModel> =
        LiteNewsDetailViewModel::class.java

    override fun initViews() {
        configureWebView()
        binding.collapse.setExpandedTitleColor(Color.TRANSPARENT)
        binding.collapse.setCollapsedTitleTextColor(Color.TRANSPARENT)
        binding.detailTitle.text = intent.getStringExtra(EXTRA_TITLE)
        intent.getStringExtra(EXTRA_TIME)?.let {
            binding.detailTime.text = Html.fromHtml(it)
        }
        viewModel.pageUrl.observe(this) { url -> binding.webview.loadUrl(url) }
    }

    override fun onStart() {
        super.onStart()
        viewModel.load(resolveUrl(intent.getStringExtra(EXTRA_LINK)))
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        binding.webview.webChromeClient = WebChromeClient()
        binding.webview.settings.mixedContentMode =
            WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        binding.webview.settings.javaScriptEnabled = true
        binding.webview.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        binding.webview.settings.loadWithOverviewMode = true
        binding.webview.addJavascriptInterface(ImageClickBridge(), JS_BRIDGE_NAME)
        binding.webview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                view.loadUrl(IMAGE_CLICK_SCRIPT)
            }
        }
    }

    /** 网页图片点击回调，交给系统浏览器打开大图。 */
    private inner class ImageClickBridge {
        @JavascriptInterface
        fun showBigImg(url: String) {
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        }
    }

    private fun resolveUrl(link: String?): String = when {
        link.isNullOrBlank() -> ""
        link.startsWith("http") -> link
        else -> NEWS_HOST + link
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_LINK = "link"
        const val EXTRA_TIME = "time"
        private const val NEWS_HOST = "http://www.hitsz.edu.cn"
        private const val JS_BRIDGE_NAME = "jsCallJavaObj"
        private val IMAGE_CLICK_SCRIPT = """
            javascript:(function(){
            var imgs=document.getElementsByTagName("img");
            for(var i=0;i<imgs.length;i++){
            imgs[i].onclick=function(){window.jsCallJavaObj.showBigImg(this.src);}
            }})()
        """.trimIndent()
    }
}
