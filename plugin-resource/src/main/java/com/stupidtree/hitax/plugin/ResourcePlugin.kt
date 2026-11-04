package com.stupidtree.hitax.plugin

import android.app.Application
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.ResourceUiService
import com.stupidtree.hita.api.SearchTab
import com.stupidtree.hita.api.SearchTabIds
import com.stupidtree.hita.plugin.resource.R
import com.stupidtree.hitax.ui.search.teacher.FragmentSearchTeacher

/**
 * 课程资源 / 教师插件。
 *
 * HOA 课程资料、教师检索与相关页面都在本模块内，宿主通过 [ResourceUiService] 访问。
 */
class ResourcePlugin(private val application: Application) : Plugin {

    override val id = "com.stupidtree.hitax.resource"

    override val displayName = "课程资源"

    override fun install(ctx: PluginContext) {
        ctx.provide(ResourceUiService.KEY, ResourceUiServiceImpl(application))

        ctx.searchTabs.register(
            SearchTab(
                id = SearchTabIds.TEACHER,
                titleRes = R.string.tab_search_teacher,
                order = 10,
            ) { FragmentSearchTeacher() }
        )
    }
}
