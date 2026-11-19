package com.stupidtree.hitax.market

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
                .replace(R.id.market_container, FragmentPluginMarket())
                .commit()
        }
    }
}