package com.stupidtree.hitax.market

import java.io.File

/** 组合条目的种类；id 只在同一种类内唯一。 */
enum class CompositionKind { BUILTIN, DEX, UI }

/** 最终值由哪一层写入；后者覆盖前者。 */
enum class CompositionSource { BASE, PRESET, USER, SAFETY }

/** 组合条目的稳定身份；id 只在同一种类内唯一。 */
data class CompositionTarget(val kind: CompositionKind, val id: String)

data class CompositionEntry(
    val id: String,
    val kind: CompositionKind,
    val enabled: Boolean,
    val required: Boolean,
    val source: CompositionSource,
)

sealed class CompositionMissingReason {
    data object NotInBase : CompositionMissingReason()
    data object NotInstalled : CompositionMissingReason()
    data object NotInIndex : CompositionMissingReason()
}

data class CompositionMissing(
    val id: String,
    val kind: CompositionKind,
    val reason: CompositionMissingReason,
)

/**
 * 一次组合计算的目标状态。
 *
 * 它是纯数据：不包含下载、安装或启用动作；materializer / reconciler 再决定怎么做。
 */
data class CompositionPlan(
    val entries: List<CompositionEntry>,
    val missing: List<CompositionMissing>,
) {
    /**
     * 会阻止应用 preset 的必选缺失。
     *
     * `NotInstalled` 是可安装缺口，必须交给 materializer；只有“索引/清单里根本没有”
     * 才是预检阶段无法解决的阻塞。
     */
    fun blockingRequiredMissing(): List<CompositionMissing> = missing.filter { item ->
        item.reason != CompositionMissingReason.NotInstalled &&
            entries.any { it.id == item.id && it.kind == item.kind && it.required }
    }

    /** 必选条目被 USER/SAFETY 层关掉时，不能静默把 preset 标成“使用中”。 */
    fun blockingRequiredDisabled(): List<CompositionEntry> =
        entries.filter { it.required && !it.enabled }

    /**
     * `applyPreset` 专属子计划：纯 BASE 条目只用于展示/依赖判断，不能因为应用 preset
     * 被重新启用。PRESET / USER / SAFETY 层仍参与对账。
     */
    fun forPresetApplication(): CompositionPlan =
        if (entries.none { it.source == CompositionSource.BASE }) {
            this
        } else {
            copy(entries = entries.filter { it.source != CompositionSource.BASE })
        }

    /** 本次对账因“切出旧组合”而自动停用的条目；不属于用户手动禁用。 */
    fun automaticDisabledTargets(): Set<CompositionTarget> =
        entries
            .filter { !it.enabled && it.source == CompositionSource.PRESET }
            .map { CompositionTarget(it.kind, it.id) }
            .toSet()

    fun dump(): String = buildString {
        appendLine("entries:")
        entries.forEach {
            appendLine(
                "  ${it.kind.name.lowercase()} ${it.id} enabled=${it.enabled} " +
                    "source=${it.source.name.lowercase()} required=${it.required}",
            )
        }
        appendLine("missing:")
        missing.forEach {
            appendLine("  ${it.kind.name.lowercase()} ${it.id} reason=${it.reason::class.simpleName}")
        }
    }.trimEnd()
}

/**
 * 只做“组合应该是什么”的计算，不读写磁盘、不访问 Android。
 *
 * 输入是当前事实（base / 已安装 / 索引可用 / 用户禁用 / kill switch）与前后两个 preset。
 * 输出覆盖 base、preset 引用以及“旧 preset 独有条目”，未提到的已安装 UI 插件保持原状。
 */
/** 选择“上一次组合”的快照：当前索引缺失时回退到上次成功落盘的 preset。 */
fun previousPresetFor(
    activeId: String?,
    nextId: String,
    live: MarketPreset?,
    saved: MarketPreset?,
): MarketPreset? {
    if (activeId == null) return null
    if (activeId == nextId) return saved?.takeIf { it.id == activeId }
    return live?.takeIf { it.id == activeId } ?: saved?.takeIf { it.id == activeId }
}

object CompositionResolver {

    fun resolve(
        baseIds: Set<String>,
        next: MarketPreset?,
        previous: MarketPreset?,
        available: Map<CompositionKind, Set<String>>,
        installed: Map<CompositionKind, Set<String>>,
        userDisabled: Map<CompositionKind, Set<String>> = emptyMap(),
        killSwitches: Set<CompositionTarget> = emptySet(),
    ): CompositionPlan {
        val entries = LinkedHashMap<Pair<CompositionKind, String>, CompositionEntry>()

        baseIds.forEach { id ->
            entries[CompositionKind.BUILTIN to id] = CompositionEntry(
                id = id,
                kind = CompositionKind.BUILTIN,
                enabled = true,
                required = false,
                source = CompositionSource.BASE,
            )
        }

        val nextRefs = next?.refs().orEmpty().distinctBy { it.compositionKey() }
        val nextKeys = nextRefs.associateBy { it.compositionKey() }
        previous?.refs().orEmpty()
            .filterNot { it.compositionKey() in nextKeys }
            // 旧组合只停用 UI/内置；dex 插件的数据与权限更重，保留给用户显式处理。
            .filter { it.kind() != CompositionKind.DEX }
            .forEach { ref ->
                val key = ref.compositionKey()
                entries[key] = CompositionEntry(
                    id = ref.id,
                    kind = key.first,
                    enabled = false,
                    required = false,
                    source = CompositionSource.PRESET,
                )
            }

        nextRefs.forEach { ref ->
            val key = ref.compositionKey()
            entries[key] = CompositionEntry(
                id = ref.id,
                kind = key.first,
                enabled = true,
                required = ref.isRequired(),
                source = CompositionSource.PRESET,
            )
        }

        val missing = mutableListOf<CompositionMissing>()
        nextRefs.forEach { ref ->
            val kind = ref.kind()
            val installedNow = kind != CompositionKind.BUILTIN && ref.id in installed[kind].orEmpty()
            val known = when (kind) {
                CompositionKind.BUILTIN -> ref.id in baseIds
                // 已安装但暂时不在索引里的插件仍可启用，不能误报为缺失。
                CompositionKind.DEX, CompositionKind.UI -> installedNow || ref.id in available[kind].orEmpty()
            }
            if (!known) {
                missing += CompositionMissing(
                    ref.id,
                    kind,
                    if (kind == CompositionKind.BUILTIN) {
                        CompositionMissingReason.NotInBase
                    } else {
                        CompositionMissingReason.NotInIndex
                    },
                )
            } else if (kind != CompositionKind.BUILTIN && !installedNow) {
                missing += CompositionMissing(ref.id, kind, CompositionMissingReason.NotInstalled)
            }
        }

        val resolved = entries.values.map { entry ->
            when {
                CompositionTarget(entry.kind, entry.id) in killSwitches -> entry.copy(
                    enabled = false,
                    source = CompositionSource.SAFETY,
                )
                entry.id in userDisabled[entry.kind].orEmpty() -> entry.copy(
                    enabled = false,
                    source = CompositionSource.USER,
                )
                else -> entry
            }
        }.sortedWith(compareBy({ it.kind.ordinal }, { it.id }))

        return CompositionPlan(
            entries = resolved,
            missing = missing.sortedWith(compareBy({ it.kind.ordinal }, { it.id })),
        )
    }

    private fun PresetPluginRef.compositionKey(): Pair<CompositionKind, String> = kind() to id

    private fun PresetPluginRef.kind(): CompositionKind = when (normalizedKind()) {
        "dex" -> CompositionKind.DEX
        "builtin" -> CompositionKind.BUILTIN
        else -> CompositionKind.UI
    }
}

/** 当前实际状态，供 reconciler 与 plan 做 diff。 */
data class CompositionState(
    val installed: Map<CompositionKind, Set<String>> = emptyMap(),
    val enabled: Map<CompositionKind, Set<String>> = emptyMap(),
) {
    fun dump(): String = buildString {
        appendLine("state:")
        installed.forEach { (kind, ids) ->
            ids.sorted().forEach { appendLine("  installed ${kind.name.lowercase()} $it") }
        }
        enabled.forEach { (kind, ids) ->
            ids.sorted().forEach { appendLine("  enabled ${kind.name.lowercase()} $it") }
        }
    }.trimEnd()
}

sealed class CompositionAction {
    data class Install(
        val id: String,
        val kind: CompositionKind,
        val required: Boolean,
    ) : CompositionAction()

    data class Enable(
        val id: String,
        val kind: CompositionKind,
        val required: Boolean,
    ) : CompositionAction()

    data class Disable(
        val id: String,
        val kind: CompositionKind,
        val required: Boolean,
    ) : CompositionAction()

    data class Blocked(
        val missing: CompositionMissing,
        val required: Boolean,
    ) : CompositionAction()
}

/**
 * materialize 阶段产出的 artifact：只包含 staged 文件，不含任何已安装状态。
 *
 * BUILTIN 没有 artifact，永远不经过 materializer。
 */
sealed interface PreparedArtifact {
    val target: CompositionTarget

    data class Ui(
        val entry: UiMarketPlugin,
        val file: File,
        val sha256: String,
    ) : PreparedArtifact {
        override val target: CompositionTarget
            get() = CompositionTarget(CompositionKind.UI, entry.id)
    }

    data class Dex(
        val entry: MarketPlugin,
        val stagedDex: File,
    ) : PreparedArtifact {
        override val target: CompositionTarget
            get() = CompositionTarget(CompositionKind.DEX, entry.id)
    }
}

/**
 * 只负责把 Install 动作需要的东西下载、校验并落到 staging。
 *
 * 不卸载旧版本、不改 `installed.json` / `ui_plugins`；这些副作用留给 reconciler。
 */
interface CompositionMaterializer {
    fun prepare(kind: CompositionKind, id: String): Result<PreparedArtifact>

    /** 成功、失败、取消路径都必须调用，清空所有 staging 文件。 */
    fun cleanup()
}

/** materialize 阶段的结果：按 target 索引已准备 artifact，并区分必选/可选失败。 */
data class PreparedComposition(
    val artifacts: Map<CompositionTarget, PreparedArtifact> = emptyMap(),
    /** 原始失败原因；成功准备后用于让 executor 在 Install 时返回同样的错误。 */
    val failures: Map<CompositionTarget, String> = emptyMap(),
    val requiredFailures: List<String> = emptyList(),
    val optionalFailures: List<String> = emptyList(),
) {
    val canReconcile: Boolean get() = requiredFailures.isEmpty()
}

/**
 * 只准备 DEX/UI 的 Install 动作；BUILTIN 由 executor 直接处理。
 *
 * 必选失败会立即停止准备（不再浪费流量），可选失败只记录并继续。
 */
internal fun materializeInstalls(
    actions: List<CompositionAction>,
    materializer: CompositionMaterializer,
): PreparedComposition {
    val artifacts = linkedMapOf<CompositionTarget, PreparedArtifact>()
    val failures = linkedMapOf<CompositionTarget, String>()
    val requiredFailures = mutableListOf<String>()
    val optionalFailures = mutableListOf<String>()

    for (action in actions) {
        if (action !is CompositionAction.Install) continue
        if (action.kind == CompositionKind.BUILTIN) continue
        val target = CompositionTarget(action.kind, action.id)
        materializer.prepare(action.kind, action.id).fold(
            onSuccess = { artifacts[target] = it },
            onFailure = { error ->
                val reason = error.message ?: "准备失败"
                failures[target] = reason
                val text = "${action.id}：$reason"
                if (action.required) requiredFailures += text else optionalFailures += text
            },
        )
        if (requiredFailures.isNotEmpty()) break
    }
    return PreparedComposition(artifacts, failures, requiredFailures, optionalFailures)
}

/** 执行副作用的最小接口；由 Android 侧适配到 installer / UiPluginManager / BuiltinPluginManager。 */
interface CompositionExecutor {
    /** 返回是否需要重建界面。 */
    fun install(kind: CompositionKind, id: String): Result<Boolean>

    fun enable(kind: CompositionKind, id: String): Result<Boolean>

    fun disable(kind: CompositionKind, id: String): Result<Boolean>

    /** 回滚到本次对账前的安装/启用状态；用于必选步骤失败后的反事实恢复。 */
    fun restore(
        kind: CompositionKind,
        id: String,
        installed: Boolean,
        enabled: Boolean,
    ): Result<Boolean>
}

data class CompositionReport(
    val installed: Int,
    val enabled: Int,
    val disabled: Int,
    val skipped: Int,
    val failures: List<String>,
    val optionalFailures: List<String>,
    val reloadNeeded: Boolean,
    val rolledBack: Int = 0,
    val rollbackFailures: List<String> = emptyList(),
) {
    val allFailures: List<String> get() = failures + optionalFailures + rollbackFailures
    val canActivate: Boolean get() = failures.isEmpty()

    fun dump(): String = buildString {
        appendLine("result:")
        appendLine("  installed=$installed enabled=$enabled disabled=$disabled skipped=$skipped")
        appendLine("  rolledBack=$rolledBack reloadNeeded=$reloadNeeded canActivate=$canActivate")
        allFailures.forEach { appendLine("  failure=$it") }
    }.trimEnd()
}

object CompositionReconciler {

    /**
     * 先安装/启用目标条目，再停用旧 preset 独有条目；后者只在必选步骤全部成功后执行。
     */
    fun actions(plan: CompositionPlan, state: CompositionState): List<CompositionAction> {
        val missingByKey = plan.missing.associateBy { it.kind to it.id }
        val activations = mutableListOf<CompositionAction>()
        val disables = mutableListOf<CompositionAction>()

        plan.entries.forEach { entry ->
            val key = entry.kind to entry.id
            val missing = missingByKey[key]
            if (!entry.enabled) {
                // 旧 preset 独有条目即使已从索引消失，也要按实际安装状态停用。
                if (entry.id in state.enabled[entry.kind].orEmpty()) {
                    disables += CompositionAction.Disable(entry.id, entry.kind, entry.required)
                }
                return@forEach
            }
            if (missing != null && missing.reason != CompositionMissingReason.NotInstalled) {
                activations += CompositionAction.Blocked(missing, entry.required)
                return@forEach
            }
            val installed = entry.id in state.installed[entry.kind].orEmpty()
            val enabled = entry.id in state.enabled[entry.kind].orEmpty()
            when {
                missing != null -> activations += CompositionAction.Install(
                    entry.id,
                    entry.kind,
                    entry.required,
                )
                !installed -> activations += CompositionAction.Install(
                    entry.id,
                    entry.kind,
                    entry.required,
                )
                !enabled -> activations += CompositionAction.Enable(
                    entry.id,
                    entry.kind,
                    entry.required,
                )
            }
        }
        return activations + disables
    }

    /**
     * materialize -> reconcile 的完整入口。
     *
     * 必选 artifact 准备失败时不执行任何 executor 动作，现有组合（active preset、
     * 已启用条目）保持原样；可选失败通过 executor 的 Install 返回错误，计入
     * [CompositionReport.optionalFailures] 后继续对账。
     */
    fun executePrepared(
        plan: CompositionPlan,
        state: CompositionState,
        materializer: CompositionMaterializer,
        executor: CompositionExecutor,
    ): CompositionReport {
        try {
            // 必选已被 USER/SAFETY 停用时不会安装任何东西，连准备都省掉。
            if (plan.blockingRequiredDisabled().isNotEmpty()) {
                return execute(plan, state, executor)
            }
            val prepared = materializeInstalls(actions(plan, state), materializer)
            if (!prepared.canReconcile) {
                return CompositionReport(
                    installed = 0,
                    enabled = 0,
                    disabled = 0,
                    skipped = plan.entries.size,
                    failures = prepared.requiredFailures,
                    optionalFailures = prepared.optionalFailures,
                    reloadNeeded = false,
                )
            }
            return execute(
                plan = plan,
                state = state,
                executor = PrepareGuardExecutor(executor, prepared.failures),
            )
        } finally {
            materializer.cleanup()
        }
    }

    fun execute(
        plan: CompositionPlan,
        state: CompositionState,
        executor: CompositionExecutor,
    ): CompositionReport {
        val disabledRequired = plan.blockingRequiredDisabled()
        if (disabledRequired.isNotEmpty()) {
            return CompositionReport(
                installed = 0,
                enabled = 0,
                disabled = 0,
                skipped = plan.entries.size,
                failures = disabledRequired.map { "${it.id}（必选插件被 ${it.source.name.lowercase()} 停用）" },
                optionalFailures = emptyList(),
                reloadNeeded = false,
            )
        }

        var installed = 0
        var enabled = 0
        var disabled = 0
        var rolledBack = 0
        var reload = false
        val failures = mutableListOf<String>()
        val optionalFailures = mutableListOf<String>()
        val rollbackFailures = mutableListOf<String>()
        data class Completed(
            val action: CompositionAction,
            val installed: Boolean,
            val enabled: Boolean,
        )
        val completed = mutableListOf<Completed>()

        fun record(required: Boolean, text: String) {
            if (required) failures += text else optionalFailures += text
        }

        fun rollback() {
            completed.asReversed().forEach { done ->
                // 用户/安全层已经决定停用的条目，不在失败回滚里重新启用。
                val source = plan.entries.firstOrNull {
                    it.id == done.action.id() && it.kind == done.action.kind()
                }?.source
                if (done.action is CompositionAction.Disable &&
                    (source == CompositionSource.USER || source == CompositionSource.SAFETY)
                ) {
                    return@forEach
                }
                val result = executor.restore(
                    done.action.kind(),
                    done.action.id(),
                    done.installed,
                    done.enabled,
                )
                if (result.isSuccess) {
                    rolledBack++
                    when (done.action) {
                        is CompositionAction.Install -> installed = (installed - 1).coerceAtLeast(0)
                        is CompositionAction.Enable -> enabled = (enabled - 1).coerceAtLeast(0)
                        is CompositionAction.Disable -> disabled = (disabled - 1).coerceAtLeast(0)
                        is CompositionAction.Blocked -> Unit
                    }
                } else {
                    rollbackFailures += "${done.action.id()}：${result.exceptionOrNull()?.message ?: "回滚失败"}"
                }
            }
        }

        val actions = actions(plan, state)
        for (action in actions) {
            if (failures.isNotEmpty()) break
            when (action) {
                is CompositionAction.Blocked -> record(
                    action.required,
                    "${action.missing.id}（${action.missing.reason.label()}）",
                )
                is CompositionAction.Install -> executor.install(action.kind, action.id)
                    .fold(
                        onSuccess = {
                            installed++
                            completed += Completed(
                                action,
                                installed = action.id in state.installed[action.kind].orEmpty(),
                                enabled = action.id in state.enabled[action.kind].orEmpty(),
                            )
                            if (it) reload = true
                        },
                        onFailure = { record(action.required, "${action.id}：${it.message ?: "安装失败"}") },
                    )
                is CompositionAction.Enable -> executor.enable(action.kind, action.id)
                    .fold(
                        onSuccess = {
                            enabled++
                            completed += Completed(
                                action,
                                installed = action.id in state.installed[action.kind].orEmpty(),
                                enabled = action.id in state.enabled[action.kind].orEmpty(),
                            )
                            if (it) reload = true
                        },
                        onFailure = { record(action.required, "${action.id}：${it.message ?: "启用失败"}") },
                    )
                is CompositionAction.Disable -> executor.disable(action.kind, action.id)
                    .onSuccess {
                        disabled++
                        completed += Completed(
                            action,
                            installed = action.id in state.installed[action.kind].orEmpty(),
                            enabled = action.id in state.enabled[action.kind].orEmpty(),
                        )
                        if (it) reload = true
                    }
                    .onFailure {
                        // 旧组合仍启用但 active preset 被改写，会形成错误事实；按失败处理并回滚。
                        record(true, "${action.id}：${it.message ?: "停用失败"}")
                    }
            }
        }
        if (failures.isNotEmpty()) rollback()

        val actionableKeys = actions.mapNotNull { action ->
            when (action) {
                is CompositionAction.Install -> action.kind to action.id
                is CompositionAction.Enable -> action.kind to action.id
                is CompositionAction.Disable -> action.kind to action.id
                is CompositionAction.Blocked -> action.missing.kind to action.missing.id
            }
        }.toSet()
        val skipped = plan.entries.count { (it.kind to it.id) !in actionableKeys }

        return CompositionReport(
            installed = installed,
            enabled = enabled,
            disabled = disabled,
            skipped = skipped,
            failures = failures,
            optionalFailures = optionalFailures,
            reloadNeeded = reload,
            rolledBack = rolledBack,
            rollbackFailures = rollbackFailures,
        )
    }

    private fun CompositionAction.kind(): CompositionKind = when (this) {
        is CompositionAction.Install -> kind
        is CompositionAction.Enable -> kind
        is CompositionAction.Disable -> kind
        is CompositionAction.Blocked -> missing.kind
    }

    private fun CompositionAction.id(): String = when (this) {
        is CompositionAction.Install -> id
        is CompositionAction.Enable -> id
        is CompositionAction.Disable -> id
        is CompositionAction.Blocked -> missing.id
    }

    private fun CompositionMissingReason.label(): String = when (this) {
        CompositionMissingReason.NotInBase -> "内置缺失"
        CompositionMissingReason.NotInstalled -> "未安装"
        CompositionMissingReason.NotInIndex -> "索引缺失"
    }

    /** 把准备阶段的失败还原成 Install 失败，避免 executor 去碰不存在的 artifact。 */
    private class PrepareGuardExecutor(
        private val delegate: CompositionExecutor,
        private val failures: Map<CompositionTarget, String>,
    ) : CompositionExecutor {
        override fun install(kind: CompositionKind, id: String): Result<Boolean> {
            failures[CompositionTarget(kind, id)]?.let { message ->
                return Result.failure(IllegalStateException(message))
            }
            return delegate.install(kind, id)
        }

        override fun enable(kind: CompositionKind, id: String): Result<Boolean> =
            delegate.enable(kind, id)

        override fun disable(kind: CompositionKind, id: String): Result<Boolean> =
            delegate.disable(kind, id)

        override fun restore(
            kind: CompositionKind,
            id: String,
            installed: Boolean,
            enabled: Boolean,
        ): Result<Boolean> = delegate.restore(kind, id, installed, enabled)
    }
}
