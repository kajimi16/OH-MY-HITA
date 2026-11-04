package com.stupidtree.hita.plugin.bangumi

import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.TabContribution

/**
 * 番剧插件：启用后向主界面贡献第五个 tab。
 *
 * 数据来自 Bangumi 公开 API，只读，不需要登录。
 */
class BangumiPlugin : Plugin {

    override val id = "com.stupidtree.hitax.bangumi"
    override val displayName = "番剧"
    override val requiresApi = 2

    override fun install(ctx: PluginContext) {
        ctx.tabs.register(
            TabContribution(
                id = TAB_ID,
                titleRes = R.string.bangumi_title,
                iconRes = R.drawable.ic_bangumi,
                order = 50,
            ) { BangumiFragment() }
        )
    }

    companion object {
        const val TAB_ID = "hita.anime"
    }
}
