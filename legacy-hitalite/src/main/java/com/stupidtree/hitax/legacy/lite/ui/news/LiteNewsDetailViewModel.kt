package com.stupidtree.hitax.legacy.lite.ui.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * HITA_Lite 新闻详情页的页面状态。
 *
 * 只保存待加载的文章地址；文章正文与元信息由 WebView 与宿主的新闻数据源处理。
 */
class LiteNewsDetailViewModel : ViewModel() {

    private val pageController = MutableLiveData<String>()

    val pageUrl: LiveData<String> = pageController

    /**
     * 前置条件：`link` 是已经补全的绝对地址。
     * 后置条件：地址非空时 `pageUrl` 读出该地址。
     */
    fun load(link: String) {
        if (link.isBlank()) return
        pageController.value = link
    }
}
