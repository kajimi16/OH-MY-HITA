package com.stupidtree.hitax.uiplugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File

/**
 * 仅 debug 包存在的开发期安装入口：跳过市场下载与 SAF，
 * 直接安装 adb 推进来的插件 APK。
 *
 * 用法见 scripts/dev-install-ui-plugin.sh。
 */
class DevUiPluginInstallerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val path = intent.getStringExtra("path") ?: return
        val expectedSha = intent.getStringExtra("sha256")
        val pending = goAsync()
        Thread {
            val result = runCatching {
                UiPluginManager(context.applicationContext).installFile(File(path), expectedSha)
            }
            Log.i(
                TAG,
                result.fold(
                    onSuccess = { "installed ${it.id}@${it.version}" },
                    onFailure = { "failed: ${it.message}" },
                )
            )
            pending.finish()
        }.start()
    }

    companion object {
        const val ACTION = "com.stupidtree.hitax.DEV_INSTALL_UI_PLUGIN"
        private const val TAG = "DevUiPlugin"
    }
}
