package com.stupidtree.hita.api

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment

/**
 * 搜索结果页的一个 tab。宿主按 [order] 排序后交给 ViewPager。
 *
 * [id] 需要稳定，宿主用它把 `SearchType` 之类的业务枚举映射到具体 tab。
 */
class SearchTab(
    val id: String,
    val titleRes: Int,
    val order: Int,
    /** 运行时插件没有资源表，用字符串标题；非空时优先于 [titleRes]。 */
    val textTitle: String? = null,
    val create: () -> Fragment,
) {
    /** 兼容 API 4 及之前编译的插件。 */
    constructor(
        id: String,
        titleRes: Int,
        order: Int,
        create: () -> Fragment,
    ) : this(id, titleRes, order, null, create)
}

/**
 * 运行时逻辑插件贡献的搜索页：宿主用固定 Fragment 承载，插件只提供 View。
 *
 * 这样旋转屏幕 / 进程恢复时 FragmentManager 只认宿主类名，不会因为插件 dex
 * 不在 classpath 而崩溃。
 */
class ViewSearchTab(
    val id: String,
    val title: String,
    val order: Int,
    val createView: (Context) -> View,
    /** 搜索框内容变化；插件可据此触发自己的查询。 */
    val onQuery: (String) -> Unit = {},
)

interface SearchTabRegistry {
    fun register(tab: SearchTab)

    fun entries(): List<SearchTab>

    /** 运行时插件使用：View 由宿主 Fragment 承载。 */
    fun registerView(tab: ViewSearchTab)

    fun viewEntries(): List<ViewSearchTab>
}

/**
 * 宿主与插件共用的语义 id。宿主用它把 `SearchType` 映射到 tab，
 * 因此插件可以随意改名实现类，不影响宿主的跳转逻辑。
 */
object SearchTabIds {
    const val TEACHER = "hita.search.teacher"
    const val USER = "hita.search.user"
    const val ARTICLE = "hita.search.article"
}
