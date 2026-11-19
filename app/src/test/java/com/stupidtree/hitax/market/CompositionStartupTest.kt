package com.stupidtree.hitax.market

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositionStartupTest {

    private val emptySets: Map<CompositionKind, Set<String>> = emptyMap()

    private fun preset(id: String, vararg refs: PresetPluginRef) =
        MarketPreset(id = id, name = id, plugins = refs.toList())

    private fun ref(id: String, kind: String, required: Boolean = true) =
        PresetPluginRef(id = id, kind = kind, required = required)

    private fun entry(
        id: String,
        kind: CompositionKind,
        enabled: Boolean,
        required: Boolean = false,
        source: CompositionSource = CompositionSource.PRESET,
    ) = CompositionEntry(id, kind, enabled, required, source)

    private fun state(
        installed: Map<CompositionKind, Set<String>> = emptyMap(),
        enabled: Map<CompositionKind, Set<String>> = emptyMap(),
    ) = CompositionState(installed, enabled)

    @Test
    fun savedPresetEnablesInstalledEntriesWithoutInstalling() {
        val saved = preset(
            "p1",
            ref("ui-a", "ui"),
            ref("builtin-a", "builtin", required = false),
        )
        val decision = planStartup(
            savedPreset = saved,
            livePreset = null,
            available = mapOf(
                CompositionKind.UI to setOf("ui-a"),
                CompositionKind.BUILTIN to setOf("builtin-a"),
            ),
            installed = mapOf(
                CompositionKind.UI to setOf("ui-a"),
                CompositionKind.BUILTIN to setOf("builtin-a"),
            ),
            enabled = emptySets,
            userDisabled = emptySets,
            killSwitches = emptySet(),
            indexAvailable = true,
        )

        assertTrue(decision.canReconcile)
        val actions = startupActions(
            decision.plan,
            state(
                installed = mapOf(
                    CompositionKind.UI to setOf("ui-a"),
                    CompositionKind.BUILTIN to setOf("builtin-a"),
                ),
            ),
        )
        assertEquals(
            listOf(
                CompositionKind.BUILTIN to "builtin-a",
                CompositionKind.UI to "ui-a",
            ),
            actions.map { action ->
                when (action) {
                    is CompositionAction.Enable -> action.kind to action.id
                    else -> error("unexpected action: $action")
                }
            },
        )

        val recorder = RecordingExecutor()
        val report = executeStartup(
            decision,
            state(
                installed = mapOf(
                    CompositionKind.UI to setOf("ui-a"),
                    CompositionKind.BUILTIN to setOf("builtin-a"),
                ),
            ),
            recorder,
        )
        assertEquals(0, report.installed)
        assertEquals(2, report.enabled)
        assertTrue(recorder.installs.isEmpty())
        assertEquals(
            setOf(
                CompositionKind.UI to "ui-a",
                CompositionKind.BUILTIN to "builtin-a",
            ),
            recorder.enables.toSet(),
        )
    }

    @Test
    fun missingCachedIndexDoesNotDisableCurrentComposition() {
        val saved = preset("p1", ref("ui-new", "ui"))
        val recorder = RecordingExecutor()
        val decision = planStartup(
            savedPreset = saved,
            livePreset = null,
            available = emptySets,
            installed = mapOf(CompositionKind.UI to setOf("ui-new")),
            enabled = mapOf(CompositionKind.UI to setOf("ui-old")),
            userDisabled = emptySets,
            killSwitches = emptySet(),
            indexAvailable = false,
            appliedEntries = listOf(
                entry("ui-old", CompositionKind.UI, enabled = true),
            ),
        )

        assertFalse(decision.canReconcile)
        assertNotNull(decision.note)
        assertTrue(decision.plan.entries.isEmpty())

        val report = executeStartup(
            decision,
            state(
                installed = mapOf(CompositionKind.UI to setOf("ui-new")),
                enabled = mapOf(CompositionKind.UI to setOf("ui-old")),
            ),
            recorder,
        )
        assertEquals(0, report.enabled)
        assertEquals(0, report.disabled)
        assertTrue(recorder.calls.isEmpty())
    }

    @Test
    fun killSwitchAndUserDisabledEntriesAreNeverTouched() {
        val saved = preset(
            "p1",
            ref("ui-killed", "ui", required = false),
            ref("ui-user", "ui", required = false),
            ref("ui-ok", "ui", required = false),
        )
        val installed = mapOf(CompositionKind.UI to setOf("ui-killed", "ui-user", "ui-ok", "ui-old"))
        val decision = planStartup(
            savedPreset = saved,
            livePreset = null,
            available = mapOf(CompositionKind.UI to setOf("ui-killed", "ui-user", "ui-ok")),
            installed = installed,
            enabled = mapOf(CompositionKind.UI to setOf("ui-old")),
            userDisabled = mapOf(CompositionKind.UI to setOf("ui-user")),
            killSwitches = setOf(CompositionTarget(CompositionKind.UI, "ui-killed")),
            indexAvailable = true,
            appliedEntries = listOf(entry("ui-old", CompositionKind.UI, enabled = true)),
        )

        assertTrue(decision.canReconcile)
        val byId = decision.plan.entries.associateBy { it.id }
        assertFalse(byId.getValue("ui-killed").enabled)
        assertEquals(CompositionSource.SAFETY, byId.getValue("ui-killed").source)
        assertFalse(byId.getValue("ui-user").enabled)
        assertEquals(CompositionSource.USER, byId.getValue("ui-user").source)
        assertTrue(byId.getValue("ui-ok").enabled)

        val actions = startupActions(decision.plan, state(installed, mapOf(CompositionKind.UI to setOf("ui-old"))))
        assertEquals(
            listOf(
                CompositionAction.Enable("ui-ok", CompositionKind.UI, false),
                CompositionAction.Disable("ui-old", CompositionKind.UI, false),
            ),
            actions,
        )
    }

    @Test
    fun requiredKillSwitchKeepsCurrentComposition() {
        val saved = preset("p1", ref("ui-killed", "ui", required = true))
        val recorder = RecordingExecutor()
        val decision = planStartup(
            savedPreset = saved,
            livePreset = null,
            available = mapOf(CompositionKind.UI to setOf("ui-killed", "ui-old")),
            installed = mapOf(CompositionKind.UI to setOf("ui-killed", "ui-old")),
            enabled = mapOf(CompositionKind.UI to setOf("ui-old")),
            userDisabled = emptySets,
            killSwitches = setOf(CompositionTarget(CompositionKind.UI, "ui-killed")),
            indexAvailable = true,
            appliedEntries = listOf(entry("ui-old", CompositionKind.UI, enabled = true)),
        )

        assertFalse(decision.canReconcile)
        assertTrue(decision.note!!.contains("ui-killed"))
        executeStartup(decision, state(), recorder)
        assertTrue(recorder.calls.isEmpty())
    }

    @Test
    fun builtinEnableAndDisableGoThroughExecutorAndAreIdempotent() {
        val saved = preset("p1", ref("builtin-new", "builtin"))
        val available = mapOf(CompositionKind.BUILTIN to setOf("builtin-new", "builtin-old"))
        val installed = mapOf(CompositionKind.BUILTIN to setOf("builtin-new", "builtin-old"))
        val applied = listOf(
            entry("builtin-old", CompositionKind.BUILTIN, enabled = true),
        )

        val first = planStartup(
            savedPreset = saved,
            livePreset = null,
            available = available,
            installed = installed,
            enabled = mapOf(CompositionKind.BUILTIN to setOf("builtin-old")),
            userDisabled = emptySets,
            killSwitches = emptySet(),
            indexAvailable = true,
            appliedEntries = applied,
        )
        assertTrue(first.canReconcile)
        val firstActions = startupActions(
            first.plan,
            state(installed, mapOf(CompositionKind.BUILTIN to setOf("builtin-old"))),
        )
        assertEquals(
            listOf(
                CompositionAction.Enable("builtin-new", CompositionKind.BUILTIN, true),
                CompositionAction.Disable("builtin-old", CompositionKind.BUILTIN, false),
            ),
            firstActions,
        )

        val recorder = RecordingExecutor()
        executeStartup(first, state(installed, mapOf(CompositionKind.BUILTIN to setOf("builtin-old"))), recorder)
        assertTrue(recorder.installs.isEmpty())
        assertEquals(listOf("builtin-new"), recorder.enables.map { it.second })
        assertEquals(listOf("builtin-old"), recorder.disables.map { it.second })
        assertEquals(
            listOf(CompositionKind.BUILTIN, CompositionKind.BUILTIN),
            recorder.calls.map { it.first },
        )

        // 第二次冷启动：状态已收敛，同样的输入不再产生任何动作。
        val second = planStartup(
            savedPreset = saved,
            livePreset = null,
            available = available,
            installed = installed,
            enabled = mapOf(CompositionKind.BUILTIN to setOf("builtin-new")),
            userDisabled = emptySets,
            killSwitches = emptySet(),
            indexAvailable = true,
            appliedEntries = applied,
        )
        val secondRecorder = RecordingExecutor()
        executeStartup(
            second,
            state(installed, mapOf(CompositionKind.BUILTIN to setOf("builtin-new"))),
            secondRecorder,
        )
        assertTrue(secondRecorder.calls.isEmpty())
    }

    @Test
    fun noSavedPresetMeansNoReconcile() {
        val decision = planStartup(
            savedPreset = null,
            livePreset = null,
            available = emptySets,
            installed = emptySets,
            enabled = emptySets,
            userDisabled = emptySets,
            killSwitches = emptySet(),
        )
        assertFalse(decision.canReconcile)
        assertEquals("没有已应用的整合包", decision.note)
        assertTrue(decision.plan.entries.isEmpty())
    }

    @Test
    fun savedPresetUsesLiveIndexVersionWhenIdsMatch() {
        val saved = preset("p1", ref("ui-old-ref", "ui"))
        val live = preset("p1", ref("ui-new-ref", "ui"))
        val decision = planStartup(
            savedPreset = saved,
            livePreset = live,
            available = mapOf(CompositionKind.UI to setOf("ui-new-ref")),
            installed = mapOf(CompositionKind.UI to setOf("ui-new-ref")),
            enabled = emptySets,
            userDisabled = emptySets,
            killSwitches = emptySet(),
            indexAvailable = true,
        )
        assertTrue(decision.canReconcile)
        assertEquals(listOf("ui-new-ref"), decision.plan.entries.map { it.id })
    }

    private class RecordingExecutor : CompositionExecutor {
        val calls = mutableListOf<Pair<CompositionKind, String>>()
        val installs = mutableListOf<Pair<CompositionKind, String>>()
        val enables = mutableListOf<Pair<CompositionKind, String>>()
        val disables = mutableListOf<Pair<CompositionKind, String>>()

        override fun install(kind: CompositionKind, id: String): Result<Boolean> {
            calls += kind to id
            installs += kind to id
            return Result.success(false)
        }

        override fun enable(kind: CompositionKind, id: String): Result<Boolean> {
            calls += kind to id
            enables += kind to id
            return Result.success(false)
        }

        override fun disable(kind: CompositionKind, id: String): Result<Boolean> {
            calls += kind to id
            disables += kind to id
            return Result.success(false)
        }

        override fun restore(
            kind: CompositionKind,
            id: String,
            installed: Boolean,
            enabled: Boolean,
        ): Result<Boolean> = Result.success(false)
    }
}
