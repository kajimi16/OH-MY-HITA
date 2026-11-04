package com.stupidtree.hitax.market

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.stupidtree.hitax.HApplication
import java.io.File

/**
 * 仅 debug 包存在：用 adb broadcast 控制运行时逻辑插件，免去点击市场。
 *
 * 依赖 `files/market/index.json`（debug 市场首次打开时会落盘）。
 */
class DevMarketReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as HApplication
        val installer = PluginInstaller(context, app.pluginHost)
        val pending = goAsync()
        Thread {
            val result = runCatching {
                when (intent.action) {
                    ACTION_INSTALL -> {
                        val id = intent.getStringExtra("id") ?: error("缺少 id")
                        val dir = File(context.filesDir, "market")
                        val index = Gson().fromJson(
                            File(dir, "index.json").readText(),
                            MarketIndex::class.java,
                        )
                        val entry = index.plugins.firstOrNull { it.id == id }
                            ?: error("索引中没有 $id")
                        installer.install(entry, dir.absolutePath)
                        "installed $id"
                    }
                    ACTION_UNINSTALL -> {
                        val id = intent.getStringExtra("id") ?: error("缺少 id")
                        installer.uninstall(id)
                        "uninstalled $id"
                    }
                    ACTION_ENABLE -> {
                        val id = intent.getStringExtra("id") ?: error("缺少 id")
                        val enabled = intent.getBooleanExtra("enabled", true)
                        installer.setEnabled(id, enabled)
                        "setEnabled $id=$enabled"
                    }
                    else -> "unknown action"
                }
            }
            Log.i(TAG, result.fold({ it }, { "failed: ${it.message}" }))
            pending.finish()
        }.start()
    }

    companion object {
        const val ACTION_INSTALL = "com.stupidtree.hitax.DEV_INSTALL_DEX"
        const val ACTION_UNINSTALL = "com.stupidtree.hitax.DEV_UNINSTALL_DEX"
        const val ACTION_ENABLE = "com.stupidtree.hitax.DEV_ENABLE_DEX"
        private const val TAG = "DevMarket"
    }
}
