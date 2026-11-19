package com.stupidtree.hita.kernel

/** 插件失败发生在哪个阶段。 */
enum class PluginFailurePhase {
    /** 元数据/API/ABI 不兼容，尚未加载类。 */
    COMPATIBILITY,

    /** 类加载、入口类实例化失败。 */
    LOAD,

    /** 入口类已实例化，但 `install()` 抛异常或内核拒绝安装。 */
    INSTALL,
}

/**
 * 已安装插件的一条诊断记录。
 *
 * [origin] / [version] / [requires] / [provides] 全部来自激活时提交的
 * [PluginDescriptor]；旧插件没有描述符时分别回落到 `UNKNOWN`、null、空集合。
 * 列表保持稳定顺序（按 id 排序，`requires` / `provides` 内部按名称排序），
 * 让导出的 JSON 可以直接比对。
 */
data class PluginSnapshot(
    val id: String,
    val requiresApi: Int,
    val origin: PluginOrigin = PluginOrigin.UNKNOWN,
    val version: String? = null,
    val requires: List<String> = emptyList(),
    val provides: List<String> = emptyList(),
)

data class PluginFailure(
    val id: String,
    val phase: PluginFailurePhase,
    val message: String,
)

data class PluginServiceBinding(
    val service: String,
    /** 插件 id，或 [PluginHost.HOST_PROVIDER]。 */
    val provider: String,
)

data class PluginWaiting(
    val id: String,
    val missing: List<String>,
)

data class PluginConflict(
    val service: String,
    val existingProvider: String,
    val rejectedProvider: String,
)

data class PluginDegraded(
    val id: String,
    val missing: List<String>,
)

data class PluginToolConflict(
    val tool: String,
    val existingPluginId: String,
    val rejectedPluginId: String,
)

data class PluginHookFailure(
    val hook: String,
    val pluginId: String,
    val listenerIndex: Int,
    val message: String,
)

/**
 * 插件在 `PluginDescriptor.provides` 里声明了某个服务，但该服务当前不由它实际提供。
 *
 * [actualProvider] 为当前的提供者（插件 id 或 [PluginHost.HOST_PROVIDER]）；
 * 没有任何提供者时为 null。该信息只用于诊断：`provides` 只是清单提示，
 * 不参与激活、等待或冲突裁决。
 */
data class PluginDeclaredProvides(
    val pluginId: String,
    val service: String,
    val actualProvider: String?,
)

/**
 * 内核当前状态的一次不可变快照。
 *
 * `waiting` / `conflicts` 在依赖声明落地后填充；当前先保留稳定字段，
 * 让诊断输出与后续实现共用同一形状。
 */
data class PluginDiagnostics(
    val installed: List<PluginSnapshot>,
    val failed: List<PluginFailure>,
    val waiting: List<PluginWaiting>,
    val conflicts: List<PluginConflict>,
    val degraded: List<PluginDegraded>,
    val services: List<PluginServiceBinding>,
    val hookFailures: List<PluginHookFailure> = emptyList(),
    val toolConflicts: List<PluginToolConflict> = emptyList(),
    /** 已安装插件的 `provides` 声明与实际提供者不一致的条目；不影响激活。 */
    val declaredProvides: List<PluginDeclaredProvides> = emptyList(),
)
