package com.stupidtree.hita.plugin.campus

import android.webkit.CookieManager

/**
 * 只读的浏览器 Cookie 视图。
 *
 * 校园扩展的登录全部发生在宿主 WebView 里，插件不实现登录流程，也不保存凭据；
 * 每次查询时从 [CookieManager] 现取，退出登录由宿主清除 Cookie 后自然反映到这里。
 */
internal class CampusCookieStore {

    /** 指定 origin 的 `name=value; ...` 原文；无法读取时返回空串。 */
    fun header(origin: String): String =
        runCatching { CookieManager.getInstance().getCookie(origin).orEmpty() }.getOrDefault("")
}
