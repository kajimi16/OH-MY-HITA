package com.stupidtree.hitax.legacy.lite

import com.stupidtree.hita.api.EasAuthService
import com.stupidtree.hita.api.EasService
import com.stupidtree.hita.api.ResourceUiService
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hita.api.UserProfileService

/**
 * 宿主服务的只读入口。
 *
 * legacy 模块不依赖 app/kernel，由 PresetShellRouter 在启动 Lite shell 前注入。
 * 页面只通过这里取服务，禁止再建 Room/Retrofit/登录单例。
 */
object LiteHostBridge {
    @Volatile var timetable: TimetableService? = null
    @Volatile var eas: EasService? = null
    @Volatile var easAuth: EasAuthService? = null
    @Volatile var userProfile: UserProfileService? = null
    @Volatile var resourceUi: ResourceUiService? = null

    fun clear() {
        timetable = null
        eas = null
        easAuth = null
        userProfile = null
        resourceUi = null
    }
}
