package com.stupidtree.hita.theta

import android.content.Intent
import android.widget.Toast
import com.stupidtree.hita.api.NavEntry
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.stupiduser.data.repository.LocalUserRepository

/**
 * θ 社区插件：目前只做一件事 —— 往宿主导航里贡献一个入口。
 *
 * 它不 import 宿主的任何类，宿主也不 import 它（除了 [com.stupidtree.hitax.kernel.PluginCatalog]
 * 这一行装配代码）。删掉装配那一行，社区入口就从抽屉里消失。
 */
class ThetaPlugin : Plugin {

    override val id: String = ID

    override val displayName: String = "θ社区"

    override fun install(ctx: PluginContext) {
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
