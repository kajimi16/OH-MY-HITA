package com.stupidtree.hita.api

import android.content.Context
import android.view.View

/**
 * 运行时界面插件（Compose 容器）。
 *
 * 与构建期插件不同：插件以独立 APK 形式安装到应用私有目录，宿主用
 * `DexClassLoader` 加载，并把它返回的 View 放进宿主 Activity。
 *
 * 约束（Android 平台边界）：
 * - 不能声明 Activity / Service / Receiver，界面只能由宿主容器承载；
 * - 不能携带自己的 XML 资源表，UI 用 Compose 代码构建；
 * - 以宿主权限运行，安装前必须校验哈希并提示能力范围。
 *
 * 构建侧约定：插件模块对 `:api` 与所有 Compose 依赖使用 `compileOnly`，
 * 由宿主提供运行时；Compose 编译器版本必须与宿主的 Kotlin 版本匹配
 * （Kotlin 1.9.22 → compose compiler 1.5.8），并在插件元数据里写 `composeVersion`。
 */
interface ComposeUiPlugin {
    val id: String
    val displayName: String

    /** 插件依赖的契约版本。 */
    val requiresApi: Int get() = 1

    /**
     * 创建插件界面。宿主负责把它放进容器并处理生命周期。
     * 实现方可以内部使用 ComposeView.setContent。
     */
    fun createView(context: Context, host: PluginUiHost): View

    /** 宿主退出时调用，用于释放监听器/协程。 */
    fun onDestroy() {}
}

/** 宿主提供给界面插件的能力。插件不能直接访问宿主的 Activity/Service。 */
interface PluginUiHost {
    fun <T : Any> service(key: ServiceKey<T>): T?

    /** 宿主打开插件时传入的参数（如搜索关键词、用户 id）。 */
    val arguments: android.os.Bundle

    /** 请求关闭插件页面。 */
    fun close()
}
