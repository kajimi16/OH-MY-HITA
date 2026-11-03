package com.stupidtree.hita.api

import android.app.Activity
import android.content.Context

/**
 * 社区能力的服务契约。
 *
 * 宿主（资料页、课件详情页、搜索页）只依赖这个接口；
 * θ 插件提供实现。插件缺席时宿主拿不到服务，相应入口静默失效，不需要改宿主代码。
 */
interface CommunityService {

    fun openUserList(context: Context, title: String, mode: String, extra: String?)

    fun openArticleList(context: Context, title: String, mode: String, extra: String?)

    /**
     * @param absolute 图片地址是否为绝对 URL。宿主从 WebView 拿到的地址是绝对的。
     */
    fun openImageGallery(activity: Activity, urls: List<String>, index: Int, absolute: Boolean)

    companion object {
        val KEY = ServiceKey<CommunityService>("hita.community")
    }
}
