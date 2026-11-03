package com.stupidtree.hita.api

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
    val create: () -> Fragment,
)

interface SearchTabRegistry {
    fun register(tab: SearchTab)

    fun entries(): List<SearchTab>
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
