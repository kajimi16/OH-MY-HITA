package com.stupidtree.hita.api

/**
 * 插件内核 API 版本号。
 *
 * 任何对 [Plugin]、[PluginContext]、[ServiceKey]、[NavEntry] 的**破坏性**改动都必须递增此值，
 * 并同步升级 build.gradle 中的 `api` 模块版本。插件在 [Plugin.requiresApi] 中声明自己依赖的版本。
 */
const val HITA_API_VERSION = 1

/**
 * 一个插件。
 *
 * 约定：
 * - 插件只能依赖 `:api`，不得依赖 `:app` 或其它插件模块。
 * - [install] 中产生的一切注册（服务、导航项、监听器）都必须通过 [PluginContext] 完成，
 *   内核会在 [uninstall] 时自动撤销。
 * - [install] 抛异常时，内核会撤销该插件已产生的全部注册，并把插件标记为加载失败，
 *   不影响其它插件。
 */
interface Plugin {
    /** 全局唯一，建议反向域名，如 `com.stupidtree.hita.theta`。 */
    val id: String

    /** 依赖的内核 API 版本。 */
    val requiresApi: Int get() = 1

    /** 人类可读的名称，用于设置页与错误日志。 */
    val displayName: String get() = id

    fun install(ctx: PluginContext)

    /** 卸载前清理非注册型资源；注册型的由内核统一回滚。 */
    fun uninstall(ctx: PluginContext) {}
}
