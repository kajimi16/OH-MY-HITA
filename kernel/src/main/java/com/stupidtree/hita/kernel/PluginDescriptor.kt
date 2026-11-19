package com.stupidtree.hita.kernel

import com.stupidtree.hita.api.Plugin

enum class PluginOrigin { BUILTIN, DEX, UNKNOWN }

/**
 * 插件在激活前可读的依赖声明。
 *
 * [requires] / [provides] 使用 `ServiceKey.name`，因为元数据要跨 dex/JSON 边界，
 * 不能引用 Kotlin 类型。没有声明的旧插件按空集合处理，行为与引入本类型前一致。
 *
 * [requires] 参与激活调度：缺失时插件进入等待队列。
 * [provides] 只是清单提示，**不参与**激活、等待或冲突裁决；实际可用服务只由
 * [PluginContext.provide] 注册的结果决定。声明与实际提供不一致时，
 * [PluginDiagnostics.declaredProvides] 会记录差异，便于发现元数据错误。
 *
 * [version] 只用于诊断展示；`Plugin` 接口不要求版本号，旧清单/旧插件按 null 处理。
 */
data class PluginDescriptor(
    val id: String,
    val requires: Set<String> = emptySet(),
    val provides: Set<String> = emptySet(),
    val origin: PluginOrigin = PluginOrigin.UNKNOWN,
    val version: String? = null,
)

/** 一次带声明的安装请求。 */
data class PluginInstallation(
    val plugin: Plugin,
    val descriptor: PluginDescriptor,
)
