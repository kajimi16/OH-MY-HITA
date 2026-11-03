package com.stupidtree.hita.api

import android.content.Context
import java.io.File

/**
 * 内核提供给插件的唯一入口。
 *
 * 每一次 [provide] / [nav] 注册都会被内核记录，插件卸载时按相反顺序撤销。
 */
interface PluginContext {
    val appContext: Context

    /** 取服务；未注册返回 null。 */
    fun <T : Any> service(key: ServiceKey<T>): T?

    /**
     * 注册服务实现。同名服务已存在时覆盖，并在卸载时恢复上一个实现。
     * 冲突策略（多个插件提供同一服务）由此处的覆盖语义 + 加载顺序决定，
     * 正式版会改为显式配置；当前阶段记录日志。
     */
    fun <T : Any> provide(key: ServiceKey<T>, impl: T)

    /**
     * 插件的私有数据目录。插件只能使用自己目录内的文件，
     * 包括各自的 Room 数据库（每个插件一个库，避免共享 @Database 的编译期实体集）。
     */
    fun filesDir(): File

    /** 导航贡献点。 */
    val nav: NavRegistry
}
