package com.stupidtree.hita.api

import com.stupidtree.hita.api.dto.EasReimportResultDto

/**
 * 教务会话命令（API 14）。
 *
 * 只承载副作用，不承载页面跳转。`startAutoReimport` 保证 listener 恰好回调一次；
 * 未登录时立即回调 `success=false`。
 */
interface EasSessionService {
    fun logout()

    /** 到达间隔且已登录、已启用自动导入时才真正导入。 */
    fun autoReimportIfDue()

    /** 恰好回调一次；回调线程由实现决定。 */
    fun startAutoReimport(listener: (EasReimportResultDto) -> Unit)

    companion object {
        val KEY = ServiceKey<EasSessionService>("hita.eas.session")
    }
}
