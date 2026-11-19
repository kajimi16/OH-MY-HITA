package com.stupidtree.hitax.market

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.File

/**
 * 组合意图与诊断快照。
 *
 * 只保存宿主安全开关和最近一次 plan/state 的 dump；插件的实际安装/启用状态仍由
 * PluginInstaller / UiPluginManager / BuiltinPluginManager 持有。
 */
object CompositionStore {
    private const val PREFS = "composition_state"
    private const val KEY_KILL_SWITCHES = "kill_switches"
    private const val KEY_APPLIED_PRESET = "applied_preset"
    private const val KEY_DESIRED = "desired_entries"
    private const val KEY_PRESET_DISABLED = "preset_disabled"
    private const val KEY_STARTUP = "startup_outcome"
    private const val DIRECTORY = "diagnostics"
    private const val SNAPSHOT = "composition.json"

    fun killSwitches(context: Context): Set<CompositionTarget> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_KILL_SWITCHES, emptySet())
            .orEmpty()
            .mapNotNull(::parseTarget)
            .toSet()

    fun isKillSwitched(context: Context, kind: CompositionKind, id: String): Boolean =
        CompositionTarget(kind, id) in killSwitches(context)

    fun setKillSwitch(context: Context, target: CompositionTarget, enabled: Boolean) {
        val next = killSwitches(context).toMutableSet()
        if (enabled) next.add(target) else next.remove(target)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_KILL_SWITCHES, next.map(::encodeTarget).toSet())
            .apply()
    }

    fun savedPreset(context: Context): MarketPreset? = runCatching {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_APPLIED_PRESET, null) ?: return null
        Gson().fromJson(json, MarketPreset::class.java)
    }.getOrNull()

    fun savePreset(context: Context, preset: MarketPreset) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APPLIED_PRESET, Gson().toJson(preset))
            .apply()
    }

    /** 最近一次组合计算的目标条目；启动期用它还原“上一次生效的组合”。 */
    fun desiredEntries(context: Context): List<CompositionEntry> = runCatching {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_DESIRED, null) ?: return emptyList()
        Gson().fromJson(json, Array<CompositionEntry>::class.java).toList()
    }.getOrDefault(emptyList())

    private fun saveDesired(context: Context, entries: List<CompositionEntry>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DESIRED, Gson().toJson(entries))
            .apply()
    }

    /**
     * 上一次整合包对账时被停用的条目。
     *
     * 这些条目不是用户手动停用的；切回包含它们的整合包时必须允许重新启用，
     * 否则 HITA-L → HITA-Android 会被自己的旧禁用标记卡住。
     */
    fun presetDisabled(context: Context): Set<CompositionTarget> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_PRESET_DISABLED)) {
            return prefs.getStringSet(KEY_PRESET_DISABLED, emptySet())
                .orEmpty()
                .mapNotNull(::parseTarget)
                .toSet()
        }
        // 升级路径：旧版本没有单独记录自动停用集合，从 desired_entries 还原并落盘。
        val derived = CompositionPlan(desiredEntries(context), emptyList()).automaticDisabledTargets()
        savePresetDisabled(context, derived)
        return derived
    }

    private fun savePresetDisabled(context: Context, targets: Set<CompositionTarget>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_PRESET_DISABLED, targets.map(::encodeTarget).toSet())
            .apply()
    }

    /** 用户手动操作后，该条目不再属于“整合包自动停用”。 */
    fun clearPresetDisabled(context: Context, target: CompositionTarget) {
        val next = presetDisabled(context) - target
        savePresetDisabled(context, next)
    }

    fun effectiveUserDisabled(
        context: Context,
        kind: CompositionKind,
        disabledIds: Set<String>,
    ): Set<String> {
        val automatic = presetDisabled(context)
            .filter { it.kind == kind }
            .map { it.id }
            .toSet()
        return disabledIds - automatic
    }

    fun record(context: Context, plan: CompositionPlan, state: CompositionState) {
        // desired 只在一次对账成功后落盘；失败时保留下一次启动可恢复的旧组合。
        write(context, snapshotJson(plan, state, report = null))
    }

    fun recordResult(
        context: Context,
        plan: CompositionPlan,
        state: CompositionState,
        report: CompositionReport,
    ) {
        if (report.canActivate) {
            saveDesired(context, plan.entries)
            savePresetDisabled(context, plan.automaticDisabledTargets())
        }
        write(context, snapshotJson(plan, state, report))
    }

    /**
     * 启动期对账结果：既写结构化偏好，也写进 composition.json 的 `startup` 段。
     */
    fun recordStartup(
        context: Context,
        plan: CompositionPlan,
        state: CompositionState,
        outcome: StartupOutcome,
    ) {
        saveStartupOutcome(context, outcome)
        write(context, snapshotJson(plan, state, report = null, startup = outcome))
    }

    fun startupOutcome(context: Context): StartupOutcome? = runCatching {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_STARTUP, null) ?: return null
        Gson().fromJson(json, StartupOutcome::class.java)
    }.getOrNull()

    fun saveStartupOutcome(context: Context, outcome: StartupOutcome) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STARTUP, Gson().toJson(outcome))
            .apply()
    }

    internal fun snapshotJson(
        plan: CompositionPlan,
        state: CompositionState,
        report: CompositionReport? = null,
        startup: StartupOutcome? = null,
    ): String = snapshot(plan, state, report, startup).toString()

    private fun snapshot(
        plan: CompositionPlan,
        state: CompositionState,
        report: CompositionReport?,
        startup: StartupOutcome? = null,
    ): JsonObject {
        val gson = Gson()
        return JsonObject().apply {
            add("desired", gson.toJsonTree(plan.entries))
            add(
                "missing",
                gson.toJsonTree(
                    plan.missing.map {
                        mapOf(
                            "id" to it.id,
                            "kind" to it.kind.name,
                            "reason" to it.reason::class.simpleName,
                        )
                    }
                ),
            )
            add("actual", gson.toJsonTree(state))
            if (report != null) add("result", gson.toJsonTree(report))
            if (startup != null) add("startup", gson.toJsonTree(startup))
        }
    }

    private fun write(context: Context, json: String) {
        val dir = File(context.filesDir, DIRECTORY).apply { mkdirs() }
        File(dir, SNAPSHOT).writeText(json)
    }

    fun read(context: Context): String? =
        File(File(context.filesDir, DIRECTORY), SNAPSHOT)
            .takeIf { it.isFile }
            ?.readText()

    private fun encodeTarget(target: CompositionTarget): String =
        "${target.kind.name}:${target.id}"

    private fun parseTarget(raw: String): CompositionTarget? = runCatching {
        val separator = raw.indexOf(':')
        if (separator <= 0) return@runCatching null
        CompositionTarget(
            CompositionKind.valueOf(raw.substring(0, separator)),
            raw.substring(separator + 1),
        )
    }.getOrNull()
}
