package com.stupidtree.hitax.market

import android.content.Context
import android.content.Intent
import com.stupidtree.hita.api.EasAuthService
import com.stupidtree.hita.api.EasService
import com.stupidtree.hita.api.ResourceUiService
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hita.api.UserProfileService
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.legacy.lite.HitaLiteActivity
import com.stupidtree.hitax.legacy.lite.LiteHostBridge
import com.stupidtree.hitax.shell.PresetShellActivity

/**
 * 把整合包声明的 shell id 映射到实际承载 Activity。
 *
 * HITA_Lite 使用宿主 legacy UI 模块的独立 Activity；其它声明 shell 继续走
 * Compose PresetShellActivity。没有 shell 的预设由调用方回到 MainActivity。
 */
object PresetShellRouter {

    const val HITA_LITE_SHELL = "hita-lite-2.0"

    fun intent(context: Context, shell: String): Intent =
        if (shell == HITA_LITE_SHELL) {
            bindLiteServices(context)
            Intent(context, HitaLiteActivity::class.java)
        } else {
            Intent(context, PresetShellActivity::class.java)
                .putExtra(PresetShellActivity.EXTRA_SHELL_ID, shell)
        }

    private fun bindLiteServices(context: Context) {
        val host = (context.applicationContext as? HApplication)?.pluginHost ?: return
        LiteHostBridge.timetable = host.service(TimetableService.KEY)
        LiteHostBridge.eas = host.service(EasService.KEY)
        LiteHostBridge.easAuth = host.service(EasAuthService.KEY)
        LiteHostBridge.userProfile = host.service(UserProfileService.KEY)
        LiteHostBridge.resourceUi = host.service(ResourceUiService.KEY)
    }
}
