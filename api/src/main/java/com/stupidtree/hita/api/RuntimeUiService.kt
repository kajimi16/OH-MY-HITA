package com.stupidtree.hita.api

import android.content.Context
import android.os.Bundle

/**
 * 宿主提供的运行时界面插件入口。
 *
 * 构建期插件（或宿主页面）在打开某个功能前先问 [open]：如果用户装了声明对应
 * capability 的运行时插件，就交给插件渲染；返回 false 时回退到内置实现。
 */
/** 运行时界面插件贡献的主界面 tab。 */
data class MainTabSpec(
    val id: String,
    val title: String,
    val order: Int = 100,
    val capability: String = RuntimeUiService.CAP_MAIN_TAB,
    /** 插件 APK 内 assets/ 下的图标路径，如 `tab_anime.png`；为空用宿主默认图标。 */
    val iconAsset: String? = null,
)

interface RuntimeUiService {
    fun isAvailable(capability: String): Boolean

    /** @return true 表示已由运行时插件接管，调用方不要再打开内置页面。 */
    fun open(context: Context, capability: String, arguments: Bundle = Bundle()): Boolean

    companion object {
        val KEY = ServiceKey<RuntimeUiService>("hita.runtime.ui")

        /** 课程资源搜索（query 参数）。 */
        const val CAP_RESOURCE_SEARCH = "hita.ui.resourceSearch"

        /** 课程资料投稿（submit 表单）；未迁移前始终回退内置页面。 */
        const val CAP_RESOURCE_CONTRIBUTE = "hita.ui.resourceContribute"

        /** 主界面第五个 tab（原型阶段只支持一个运行时页面）。 */
        const val CAP_MAIN_TAB = "hita.ui.mainTab"
    }
}
