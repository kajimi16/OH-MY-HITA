package com.stupidtree.hita.theta

import android.content.Intent
import android.widget.Toast
import com.stupidtree.hita.api.CommunityService
import com.stupidtree.hita.api.HostUiService
import com.stupidtree.hita.api.NavEntry
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.SearchTab
import com.stupidtree.hita.api.SearchTabIds
import com.stupidtree.hita.theta.ui.list.ArticleListFragment
import com.stupidtree.hita.theta.ui.user.UserListFragment
import com.stupidtree.stupiduser.data.repository.LocalUserRepository

/**
 * θ 社区插件。它向宿主贡献三样东西：
 * 1. [CommunityService] —— 资料页/详情页的社区入口；
 * 2. 搜索结果页的「用户」「帖子」两个 tab；
 * 3. 抽屉里的「θ社区」入口。
 *
 * 插件不 import 宿主的任何类；宿主也不 import 它，除了
 * [com.stupidtree.hitax.kernel.PluginCatalog] 里那一行装配代码。
 * 删掉那一行，三样贡献同时消失，宿主无需改动。
 */
class ThetaPlugin : Plugin {

    override val id: String = ID

    override val displayName: String = "θ社区"

    /** θ 只消费远端接口与宿主共享的账号数据。 */
    override fun purgeData() = Unit

    override fun install(ctx: PluginContext) {
        ctx.provide(CommunityService.KEY, ThetaCommunityService())
        ThetaRuntime.hostUi = ctx.service(HostUiService.KEY)

        ctx.searchTabs.register(
            SearchTab(
                id = SearchTabIds.USER,
                titleRes = R.string.plugin_search_user,
                order = 20,
            ) { UserListFragment.newInstance("search", "") }
        )
        ctx.searchTabs.register(
            SearchTab(
                id = SearchTabIds.ARTICLE,
                titleRes = R.string.plugin_search_article,
                order = 30,
            ) { ArticleListFragment.newInstance("search", "") }
        )

        ctx.nav.register(
            NavEntry(
                id = NAV_COMMUNITY,
                titleRes = R.string.plugin_theta_title,
                iconRes = R.drawable.ic_discover,
                order = 30,
            ) { context ->
                if (LocalUserRepository.getInstance(context).getLoggedInUser().isValid()) {
                    Intent(context, ThetaActivity::class.java)
                } else {
                    Toast.makeText(context, R.string.log_in_first, Toast.LENGTH_SHORT).show()
                    null
                }
            }
        )
    }

    companion object {
        const val ID = "com.stupidtree.hita.theta"
        const val NAV_COMMUNITY = "theta.community"
    }
}
