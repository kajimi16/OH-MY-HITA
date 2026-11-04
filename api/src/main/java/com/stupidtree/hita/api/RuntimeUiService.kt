package com.stupidtree.hita.api

import android.content.Context
import android.os.Bundle

/**
 * 宿主提供的运行时界面插件入口。
 *
 * 构建期插件（或宿主页面）在打开某个功能前先问 [open]：如果用户装了声明对应
 * capability 的运行时插件，就交给插件渲染；返回 false 时回退到内置实现。
 */
interface RuntimeUiService {
    fun isAvailable(capability: String): Boolean

    /** @return true 表示已由运行时插件接管，调用方不要再打开内置页面。 */
    fun open(context: Context, capability: String, arguments: Bundle = Bundle()): Boolean

    companion object {
        val KEY = ServiceKey<RuntimeUiService>("hita.runtime.ui")

        /** 课程资源搜索（query/submit 参数）。 */
        const val CAP_RESOURCE_SEARCH = "hita.ui.resourceSearch"
    }
}
