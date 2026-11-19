package com.stupidtree.hita.api

import java.io.File

/**
 * 内核提供给插件的唯一入口。
 *
 * 每一次 [provide] / [nav] / [searchTabs] / [sync] 注册都会被内核记录，
 * 插件卸载时按相反顺序撤销。
 */
interface PluginContext {

    /** 取服务；未注册返回 null。 */
    fun <T : Any> service(key: ServiceKey<T>): T?

    /**
     * 注册服务实现。
     *
     * 宿主通过 `provideGlobal` 提供的服务永远优先；插件之间先提供者胜出，
     * 后来者对该 key 的注册会被拒绝并记录冲突，不再静默覆盖。
     * 同一个插件重复注册同一 key 仍会覆盖自身旧实现，卸载时恢复。
     */
    fun <T : Any> provide(key: ServiceKey<T>, impl: T)

    /**
     * 插件的私有数据目录。插件只能使用自己目录内的文件，
     * 包括各自的 Room 数据库（每个插件一个库，避免共享 @Database 的编译期实体集）。
     */
    fun filesDir(): File

    /** 同步决策 hook；注册随插件卸载撤销。 */
    val hooks: PluginHooks

    /** 面向模型的工具贡献点；注册随插件卸载撤销。 */
    val tools: PluginToolRegistry

    /** 导航贡献点。 */
    val nav: NavRegistry

    /** 搜索结果页 tab 贡献点。 */
    val searchTabs: SearchTabRegistry

    /** 云同步数据表贡献点。 */
    val sync: SyncRegistry

    /** 主界面 tab 贡献点。 */
    val tabs: TabRegistry
}
