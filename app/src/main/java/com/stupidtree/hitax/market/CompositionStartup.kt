package com.stupidtree.hitax.market

/**
 * 启动期按已应用的整合包重放一次组合。
 *
 * 这里只有纯计算：输入是持久化的整合包/上一次 plan、缓存索引可用性与当前安装/启用状态；
 * 输出是“能不能对账”以及只含 enable/disable 的目标计划。下载、安装 DEX/UI 永远不在
 * 启动期发生，缺 artifact 时保持当前可用组合不动。
 */
data class StartupDecision(
    val plan: CompositionPlan,
    val canReconcile: Boolean,
    val skipped: List<CompositionMissing>,
    /** 不能对账时的原因；用于 composition.json 与插件诊断。 */
    val note: String? = null,
)

/** 启动结果的持久化形态；全部字段都能被 Gson 直接反序列化。 */
data class StartupOutcome(
    val attempted: Boolean = false,
    val canReconcile: Boolean = false,
    val note: String? = null,
    val skipped: List<String> = emptyList(),
    val enabled: Int = 0,
    val disabled: Int = 0,
    val failures: List<String> = emptyList(),
    val at: Long = 0L,
)

private const val STARTUP_PREVIOUS_ID = "__startup_previous__"

/**
 * 计算启动决策。
 *
 * - `savedPreset`：上一次成功应用、并写入 [PresetStateStore] 的整合包（结构化数据）。
 * - `livePreset`：缓存索引里的同 id 整合包；存在时优先用它的清单。
 * - `appliedEntries`：最近一次 plan 的 desired 条目，用来还原“上一次生效的组合”，
 *   避免把用户手工启用/宿主基础的插件误当成旧组合停用。
 * - `indexAvailable`：缓存索引是否存在。缺失时直接放弃对账，绝不拆掉当前组合。
 *
 * 可对账的前提：有 saved preset、有缓存索引、必选条目都已安装且未被 USER/SAFETY 停用。
 */
fun planStartup(
    savedPreset: MarketPreset?,
    livePreset: MarketPreset?,
    available: Map<CompositionKind, Set<String>>,
    installed: Map<CompositionKind, Set<String>>,
    enabled: Map<CompositionKind, Set<String>>,
    userDisabled: Map<CompositionKind, Set<String>>,
    killSwitches: Set<CompositionTarget>,
    indexAvailable: Boolean = true,
    appliedEntries: List<CompositionEntry> = emptyList(),
): StartupDecision {
    val emptyPlan = CompositionPlan(entries = emptyList(), missing = emptyList())
    if (savedPreset == null) {
        return StartupDecision(emptyPlan, false, emptyList(), "没有已应用的整合包")
    }
    if (!indexAvailable) {
        return StartupDecision(emptyPlan, false, emptyList(), "缓存索引缺失，保持当前组合")
    }
    val next = livePreset?.takeIf { it.id == savedPreset.id } ?: savedPreset
    if (next.refs().isEmpty()) {
        return StartupDecision(emptyPlan, false, emptyList(), "整合包没有插件清单")
    }

    val baseIds = available[CompositionKind.BUILTIN].orEmpty() +
        installed[CompositionKind.BUILTIN].orEmpty()
    val previous = previousComposition(enabled, appliedEntries)
    val full = CompositionResolver.resolve(
        baseIds = baseIds,
        next = next,
        previous = previous,
        available = available,
        installed = installed,
        userDisabled = userDisabled,
        killSwitches = killSwitches,
    ).forPresetApplication()

    // 启动期不 install：目标条目没安装时只记诊断，不进入 executor。
    val actionable = full.copy(
        entries = full.entries.filter { entry ->
            !entry.enabled ||
                entry.kind == CompositionKind.BUILTIN ||
                entry.id in installed[entry.kind].orEmpty()
        },
    )

    val requiredMissing = full.blockingRequiredMissing()
    val requiredNotInstalled = full.entries.filter { entry ->
        entry.enabled && entry.required &&
            entry.kind != CompositionKind.BUILTIN &&
            entry.id !in installed[entry.kind].orEmpty()
    }
    val requiredDisabled = full.blockingRequiredDisabled()
    val note = when {
        requiredMissing.isNotEmpty() ->
            "必选条目不在索引/清单：" + requiredMissing.joinToString { it.id }
        requiredNotInstalled.isNotEmpty() ->
            "必选插件未安装：" + requiredNotInstalled.joinToString { it.id }
        requiredDisabled.isNotEmpty() ->
            "必选插件被停用：" + requiredDisabled.joinToString { it.id }
        else -> null
    }

    return StartupDecision(
        plan = actionable,
        canReconcile = note == null,
        skipped = full.missing.distinctBy { it.kind to it.id },
        note = note,
    )
}

/**
 * 启动期只允许 enable/disable；任何 Install 动作（理论上不会出现）都被过滤掉。
 */
fun startupActions(plan: CompositionPlan, state: CompositionState): List<CompositionAction> =
    CompositionReconciler.actions(plan, state)
        .filterNot { it is CompositionAction.Install }

/** 执行启动决策；不能对账时只返回诊断报告，不触碰任何 executor。 */
fun executeStartup(
    decision: StartupDecision,
    state: CompositionState,
    executor: CompositionExecutor,
): CompositionReport {
    if (!decision.canReconcile) {
        return CompositionReport(
            installed = 0,
            enabled = 0,
            disabled = 0,
            skipped = decision.plan.entries.size + decision.skipped.size,
            failures = emptyList(),
            optionalFailures = emptyList(),
            reloadNeeded = false,
        )
    }
    return CompositionReconciler.execute(decision.plan, state, executor)
}

/** 把启动结果转成可持久化的字符串列表（missing 原因用类名）。 */
fun StartupDecision.diagnosticLines(): List<String> =
    skipped.map { "${it.kind.name.lowercase()}:${it.id}:${it.reason::class.simpleName}" } +
        listOfNotNull(note)

private fun previousComposition(
    enabled: Map<CompositionKind, Set<String>>,
    applied: List<CompositionEntry>,
): MarketPreset? {
    val refs = LinkedHashMap<Pair<CompositionKind, String>, PresetPluginRef>()
    applied.filter { it.enabled && it.source != CompositionSource.BASE }
        .forEach { entry ->
            refs[entry.kind to entry.id] = entry.toRef()
        }
    if (applied.isEmpty()) {
        // 升级路径：没有结构化 desired 时只把“当前启用的 UI”视作旧组合；
        // 内置插件不可靠地猜，宁可不停用宿主基础能力。
        enabled[CompositionKind.UI].orEmpty().forEach { id ->
            refs[CompositionKind.UI to id] = PresetPluginRef(id, "ui")
        }
    }
    if (refs.isEmpty()) return null
    return MarketPreset(
        id = STARTUP_PREVIOUS_ID,
        name = "",
        plugins = refs.values.toList(),
    )
}

private fun CompositionEntry.toRef(): PresetPluginRef = PresetPluginRef(
    id = id,
    kind = when (kind) {
        CompositionKind.BUILTIN -> "builtin"
        CompositionKind.DEX -> "dex"
        CompositionKind.UI -> "ui"
    },
)
