package com.stupidtree.hitax.market

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositionReconcilerTest {

    private class FakeExecutor(
        private val failInstalls: Set<String> = emptySet(),
        private val failEnables: Set<String> = emptySet(),
        private val failDisables: Set<String> = emptySet(),
    ) : CompositionExecutor {
        val calls = mutableListOf<String>()

        override fun install(kind: CompositionKind, id: String): Result<Boolean> {
            calls += "install:${kind.name}:$id"
            return if (id in failInstalls) Result.failure(IllegalStateException("install failed"))
            else Result.success(kind == CompositionKind.UI)
        }

        override fun enable(kind: CompositionKind, id: String): Result<Boolean> {
            calls += "enable:${kind.name}:$id"
            return if (id in failEnables) Result.failure(IllegalStateException("enable failed"))
            else Result.success(kind == CompositionKind.UI)
        }

        override fun disable(kind: CompositionKind, id: String): Result<Boolean> {
            calls += "disable:${kind.name}:$id"
            return if (id in failDisables) Result.failure(IllegalStateException("disable failed"))
            else Result.success(kind == CompositionKind.UI)
        }

        override fun restore(
            kind: CompositionKind,
            id: String,
            installed: Boolean,
            enabled: Boolean,
        ): Result<Boolean> {
            calls += "restore:${kind.name}:$id:$installed:$enabled"
            return Result.success(true)
        }
    }

    private fun entry(
        id: String,
        kind: CompositionKind,
        enabled: Boolean = true,
        required: Boolean = false,
        source: CompositionSource = CompositionSource.PRESET,
    ) = CompositionEntry(id, kind, enabled, required, source)

    private fun plan(
        entries: List<CompositionEntry>,
        missing: List<CompositionMissing> = emptyList(),
    ) = CompositionPlan(entries, missing)

    private fun state(
        installed: Map<CompositionKind, Set<String>> = emptyMap(),
        enabled: Map<CompositionKind, Set<String>> = emptyMap(),
    ) = CompositionState(installed, enabled)

    @Test
    fun installsMissingArtifactBeforeDisablingOldEntries() {
        val executor = FakeExecutor()
        val result = CompositionReconciler.execute(
            plan = plan(
                listOf(
                    entry("new-ui", CompositionKind.UI, required = true),
                    entry("old-ui", CompositionKind.UI, enabled = false),
                ),
                missing = listOf(
                    CompositionMissing("new-ui", CompositionKind.UI, CompositionMissingReason.NotInstalled),
                ),
            ),
            state = state(
                installed = mapOf(CompositionKind.UI to setOf("old-ui")),
                enabled = mapOf(CompositionKind.UI to setOf("old-ui")),
            ),
            executor = executor,
        )

        assertEquals(listOf("install:UI:new-ui", "disable:UI:old-ui"), executor.calls)
        assertEquals(1, result.installed)
        assertEquals(1, result.disabled)
        assertTrue(result.reloadNeeded)
        assertTrue(result.canActivate)
    }

    @Test
    fun disabledOldEntryStillDisablesEvenWhenNoLongerInIndex() {
        val executor = FakeExecutor()
        val result = CompositionReconciler.execute(
            plan = plan(
                listOf(entry("old-ui", CompositionKind.UI, enabled = false)),
                missing = listOf(
                    CompositionMissing("old-ui", CompositionKind.UI, CompositionMissingReason.NotInIndex),
                ),
            ),
            state = state(
                installed = mapOf(CompositionKind.UI to setOf("old-ui")),
                enabled = mapOf(CompositionKind.UI to setOf("old-ui")),
            ),
            executor = executor,
        )

        assertEquals(listOf("disable:UI:old-ui"), executor.calls)
        assertEquals(1, result.disabled)
        assertTrue(result.failures.isEmpty())
    }

    @Test
    fun requiredBlockedFailureSkipsOldEntryDisables() {
        val executor = FakeExecutor()
        val result = CompositionReconciler.execute(
            plan = plan(
                listOf(
                    entry("ghost", CompositionKind.UI, required = true),
                    entry("old-ui", CompositionKind.UI, enabled = false),
                ),
                missing = listOf(
                    CompositionMissing("ghost", CompositionKind.UI, CompositionMissingReason.NotInIndex),
                ),
            ),
            state = state(
                installed = mapOf(CompositionKind.UI to setOf("old-ui")),
                enabled = mapOf(CompositionKind.UI to setOf("old-ui")),
            ),
            executor = executor,
        )

        assertTrue(executor.calls.isEmpty())
        assertEquals(1, result.failures.size)
        assertFalse(result.canActivate)
    }

    @Test
    fun requiredInstallFailureRollsBackEarlierSuccessfulActions() {
        val executor = FakeExecutor(failInstalls = setOf("second"))
        val result = CompositionReconciler.execute(
            plan = plan(
                listOf(
                    entry("first", CompositionKind.UI, required = true),
                    entry("second", CompositionKind.UI, required = true),
                ),
                missing = listOf(
                    CompositionMissing("first", CompositionKind.UI, CompositionMissingReason.NotInstalled),
                    CompositionMissing("second", CompositionKind.UI, CompositionMissingReason.NotInstalled),
                ),
            ),
            state = state(),
            executor = executor,
        )

        assertEquals(
            listOf("install:UI:first", "install:UI:second", "restore:UI:first:false:false"),
            executor.calls,
        )
        assertEquals(0, result.installed)
        assertEquals(1, result.rolledBack)
        assertFalse(result.canActivate)
    }

    @Test
    fun disableFailureBlocksActivationAndRollsBackActivations() {
        val executor = FakeExecutor(failDisables = setOf("old-ui"))
        val result = CompositionReconciler.execute(
            plan = plan(
                listOf(
                    entry("new-ui", CompositionKind.UI, required = true),
                    entry("old-ui", CompositionKind.UI, enabled = false),
                ),
                missing = listOf(
                    CompositionMissing("new-ui", CompositionKind.UI, CompositionMissingReason.NotInstalled),
                ),
            ),
            state = state(
                installed = mapOf(CompositionKind.UI to setOf("old-ui")),
                enabled = mapOf(CompositionKind.UI to setOf("old-ui")),
            ),
            executor = executor,
        )

        assertEquals(
            listOf("install:UI:new-ui", "disable:UI:old-ui", "restore:UI:new-ui:false:false"),
            executor.calls,
        )
        assertFalse(result.canActivate)
        assertEquals(1, result.rolledBack)
    }

    @Test
    fun rollbackDoesNotReenableUserDisabledEntries() {
        val executor = FakeExecutor(failDisables = setOf("preset-old"))
        val result = CompositionReconciler.execute(
            plan = plan(
                listOf(
                    entry("new", CompositionKind.UI, required = true),
                    entry(
                        "user-old",
                        CompositionKind.UI,
                        enabled = false,
                        source = CompositionSource.USER,
                    ),
                    entry(
                        "preset-old",
                        CompositionKind.UI,
                        enabled = false,
                        source = CompositionSource.PRESET,
                    ),
                ),
                missing = listOf(
                    CompositionMissing("new", CompositionKind.UI, CompositionMissingReason.NotInstalled),
                ),
            ),
            state = state(
                installed = mapOf(CompositionKind.UI to setOf("user-old", "preset-old")),
                enabled = mapOf(CompositionKind.UI to setOf("user-old", "preset-old")),
            ),
            executor = executor,
        )

        assertEquals(
            listOf(
                "install:UI:new",
                "disable:UI:user-old",
                "disable:UI:preset-old",
                "restore:UI:new:false:false",
            ),
            executor.calls,
        )
        assertFalse(result.canActivate)
    }

    @Test
    fun requiredDisabledEntryIsBlockedBeforeAnyExecutorCall() {
        val executor = FakeExecutor()
        val result = CompositionReconciler.execute(
            plan = plan(
                listOf(entry("required", CompositionKind.UI, enabled = false, required = true)),
            ),
            state = state(),
            executor = executor,
        )

        assertTrue(executor.calls.isEmpty())
        assertFalse(result.canActivate)
        assertEquals(1, result.skipped)
    }

    @Test
    fun optionalBlockedFailureStillDisablesOldEntries() {
        val executor = FakeExecutor()
        val result = CompositionReconciler.execute(
            plan = plan(
                listOf(
                    entry("ghost", CompositionKind.UI, required = false),
                    entry("old-ui", CompositionKind.UI, enabled = false),
                ),
                missing = listOf(
                    CompositionMissing("ghost", CompositionKind.UI, CompositionMissingReason.NotInIndex),
                ),
            ),
            state = state(
                installed = mapOf(CompositionKind.UI to setOf("old-ui")),
                enabled = mapOf(CompositionKind.UI to setOf("old-ui")),
            ),
            executor = executor,
        )

        assertEquals(listOf("disable:UI:old-ui"), executor.calls)
        assertTrue(result.failures.isEmpty())
        assertEquals(1, result.optionalFailures.size)
        assertTrue(result.canActivate)
    }

    @Test
    fun alreadyInstalledButDisabledEntryIsEnabled() {
        val executor = FakeExecutor()
        val result = CompositionReconciler.execute(
            plan = plan(listOf(entry("ui", CompositionKind.UI))),
            state = state(
                installed = mapOf(CompositionKind.UI to setOf("ui")),
                enabled = emptyMap(),
            ),
            executor = executor,
        )

        assertEquals(listOf("enable:UI:ui"), executor.calls)
        assertEquals(1, result.enabled)
        assertEquals(0, result.skipped)
    }

    @Test
    fun installFailureIsClassifiedByRequiredFlag() {
        val executor = FakeExecutor(failInstalls = setOf("required", "optional"))
        val result = CompositionReconciler.execute(
            plan = plan(
                listOf(
                    entry("optional", CompositionKind.DEX, required = false),
                    entry("required", CompositionKind.DEX, required = true),
                ),
                missing = listOf(
                    CompositionMissing("optional", CompositionKind.DEX, CompositionMissingReason.NotInstalled),
                    CompositionMissing("required", CompositionKind.DEX, CompositionMissingReason.NotInstalled),
                ),
            ),
            state = state(),
            executor = executor,
        )

        assertEquals(1, result.failures.size)
        assertEquals(1, result.optionalFailures.size)
        assertFalse(result.canActivate)
    }
}
