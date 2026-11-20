package com.stupidtree.hita.api

import android.content.Context

/**
 * 宿主环境的最小暴露面。
 *
 * T1 插件需要 Room/系统服务时使用 applicationContext，不能拿到 Activity。
 * 该服务由宿主全局提供，随 API 11 引入。
 */
interface HostEnvironmentService {
    val applicationContext: Context

    companion object {
        val KEY = ServiceKey<HostEnvironmentService>("hita.host.environment")
    }
}
