package com.stupidtree.hitax.market

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import com.stupidtree.hitax.R

/**
 * 插件市场的独立页面。
 *
 * 复刻型整合包（如 hita-android-3.1.2）的底栏不该出现“插件市场”这个 3.1.2 不存在的 tab，
 * 因此把市场从主界面 pager 里挪出来：内容仍是 [FragmentPluginMarket]，
 * 由抽屉入口拉起本 Activity，功能与角色不变。
 */
class PluginMarketActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.stupidtree.hitax.analytics.UsageAnalytics.record(
            this,
            com.stupidtree.hitax.analytics.UsageEventAllowlist.PLUGIN_MARKET_VIEW,
        )
        val container = FragmentContainerView(this).apply {
            id = R.id.market_container
        }
        setContentView(container)
        if (supportFragmentManager.findFragmentById(R.id.market_container) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.market_container, FragmentPluginMarket().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PLUGIN_ID, intent.getStringExtra(EXTRA_PLUGIN_ID))
                        putBoolean(ARG_AUTO_INSTALL, intent.getBooleanExtra(EXTRA_AUTO_INSTALL, false))
                    }
                })
                .commit()
        }
    }

    companion object {
        const val EXTRA_PLUGIN_ID = "plugin_id"
        const val EXTRA_AUTO_INSTALL = "auto_install"
        const val ARG_PLUGIN_ID = "arg_plugin_id"
        const val ARG_AUTO_INSTALL = "arg_auto_install"

        fun intent(context: Context, pluginId: String, autoInstall: Boolean = false): Intent =
            Intent(context, PluginMarketActivity::class.java)
                .putExtra(EXTRA_PLUGIN_ID, pluginId)
                .putExtra(EXTRA_AUTO_INSTALL, autoInstall)
    }
}