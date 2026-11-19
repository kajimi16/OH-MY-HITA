package com.stupidtree.hitax.uiplugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * 仅 debug 包存在：用 adb broadcast 直接打开运行时插件页面，跳过 UI 导航。
 *
 * adb shell am broadcast -a com.stupidtree.hitax.DEV_OPEN_UI_PLUGIN \
 *   -n <pkg>/com.stupidtree.hitax.uiplugin.DevUiPluginOpenReceiver \
 *   --es id com.stupidtree.hita.ui.resource --es query CS
 */
class DevUiPluginOpenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val id = intent.getStringExtra("id") ?: return
        val args = Bundle().apply {
            putString("query", intent.getStringExtra("query"))
            putBoolean("submit", intent.getBooleanExtra("submit", false))
        }
        context.startActivity(
            Intent(context, PluginUiActivity::class.java)
                .putExtra(PluginUiActivity.EXTRA_PLUGIN_ID, id)
                .putExtra(PluginUiActivity.EXTRA_ARGUMENTS, args)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    companion object {
        const val ACTION = "com.stupidtree.hitax.DEV_OPEN_UI_PLUGIN"
    }
}
