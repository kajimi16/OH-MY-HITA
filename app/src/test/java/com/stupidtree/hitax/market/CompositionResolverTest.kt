package com.stupidtree.hitax.market

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositionResolverTest {

    private fun preset(
        id: String,
        vararg refs: PresetPluginRef,
    ) = MarketPreset(id = id, name = id, plugins = refs.toList())

    private fun resolve(
        base: Set<String> = emptySet(),
        next: MarketPreset? = null,
        previous: MarketPreset? = null,
        available: Map<CompositionKind, Set<String>> = emptyMap(),
        installed: Map<CompositionKind, Set<String>> = emptyMap(),
        userDisabled: Map<CompositionKind, Set<String>> = emptyMap(),
        killSwitches: Set<CompositionTarget> = emptySet(),
    ) = CompositionResolver.resolve(
        baseIds = base,
        next = next,
        previous = previous,
        available = available,
        installed = installed,
        userDisabled = userDisabled,
        killSwitches = killSwitches,
    )

    private fun CompositionPlan.entry(id: String, kind: CompositionKind): CompositionEntry =
        entries.single { it.id == id && it.kind == kind }

    @Test
    fun baseEntriesAreEnabledByDefault() {
        val plan = resolve(base = setOf("timetable", "eas"))

        assertEquals(
            listOf("eas", "timetable"),
            plan.entries.map { it.id },
        )
        assertTrue(plan.entries.all { it.enabled && it.source == CompositionSource.BASE })
        assertTrue(plan.missing.isEmpty())
    }

    @Test
    fun presetEnablesRefsAndDisablesPreviousUniqueEntries() {
        val previous = preset(
            "old",
            PresetPluginRef("old-ui", "ui"),
            PresetPluginRef("shared-ui", "ui"),
        )
        val next = preset(
            "new",
            PresetPluginRef("shared-ui", "ui", required = true),
            PresetPluginRef("new-dex", "dex"),
        )

        val plan = resolve(
            next = next,
            previous = previous,
            available = mapOf(
                CompositionKind.UI to setOf("shared-ui"),
                CompositionKind.DEX to setOf("new-dex"),
            ),
            installed = mapOf(
                CompositionKind.UI to setOf("old-ui", "shared-ui"),
                CompositionKind.DEX to setOf("new-dex"),
            ),
        )

        assertFalse(plan.entry("old-ui", CompositionKind.UI).enabled)
        assertTrue(plan.entry("shared-ui", CompositionKind.UI).enabled)
        assertEquals(CompositionSource.PRESET, plan.entry("shared-ui", CompositionKind.UI).source)
        assertTrue(plan.entry("shared-ui", CompositionKind.UI).required)
    }

    @Test
    fun userDisableOverridesPresetEnable() {
        val plan = resolve(
            next = preset("new", PresetPluginRef("shared-ui", "ui")),
            available = mapOf(CompositionKind.UI to setOf("shared-ui")),
            installed = mapOf(CompositionKind.UI to setOf("shared-ui")),
            userDisabled = mapOf(CompositionKind.UI to setOf("shared-ui")),
        )

        val entry = plan.entry("shared-ui", CompositionKind.UI)
        assertFalse(entry.enabled)
        assertEquals(CompositionSource.USER, entry.source)
    }

    @Test
    fun killSwitchOverridesUserDisableAndPreset() {
        val plan = resolve(
            base = setOf("timetable"),
            next = preset("new", PresetPluginRef("shared-ui", "ui")),
            available = mapOf(CompositionKind.UI to setOf("shared-ui")),
            installed = mapOf(CompositionKind.UI to setOf("shared-ui")),
            userDisabled = mapOf(CompositionKind.UI to setOf("shared-ui")),
            killSwitches = setOf(CompositionTarget(CompositionKind.UI, "shared-ui")),
        )

        assertEquals(CompositionSource.SAFETY, plan.entry("shared-ui", CompositionKind.UI).source)
        assertFalse(plan.entry("shared-ui", CompositionKind.UI).enabled)
    }

    @Test
    fun previousPresetFallsBackToSavedWhenIndexDroppedIt() {
        val old = preset("old", PresetPluginRef("old-ui", "ui"))

        assertEquals(old, previousPresetFor("old", "new", live = null, saved = old))
    }

    @Test
    fun samePresetReapplyUsesSavedRefs() {
        val old = preset("same", PresetPluginRef("old-ui", "ui"))
        val changed = preset("same", PresetPluginRef("new-ui", "ui"))

        assertEquals(old, previousPresetFor("same", changed.id, live = changed, saved = old))
    }

    @Test
    fun repeatedResolveIsDeterministic() {
        val next = preset(
            "new",
            PresetPluginRef("b-ui", "ui"),
            PresetPluginRef("a-dex", "dex"),
        )
        val first = resolve(
            base = setOf("timetable", "eas"),
            next = next,
            available = mapOf(
                CompositionKind.UI to setOf("b-ui"),
                CompositionKind.DEX to setOf("a-dex"),
            ),
        )
        val second = resolve(
            base = setOf("eas", "timetable"),
            next = next,
            available = mapOf(
                CompositionKind.UI to setOf("b-ui"),
                CompositionKind.DEX to setOf("a-dex"),
            ),
        )
        assertEquals(first, second)
    }

    @Test
    fun killSwitchIsKindScoped() {
        val plan = resolve(
            base = setOf("shared"),
            next = preset("new", PresetPluginRef("shared", "ui")),
            available = mapOf(CompositionKind.UI to setOf("shared")),
            killSwitches = setOf(CompositionTarget(CompositionKind.UI, "shared")),
        )

        assertTrue(plan.entry("shared", CompositionKind.BUILTIN).enabled)
        assertFalse(plan.entry("shared", CompositionKind.UI).enabled)
        assertEquals(CompositionSource.SAFETY, plan.entry("shared", CompositionKind.UI).source)
    }

    @Test
    fun missingReasonsSeparateUnknownIndexFromNotInstalled() {
        val plan = resolve(
            next = preset(
                "new",
                PresetPluginRef("missing-ui", "ui"),
                PresetPluginRef("known-dex", "dex"),
                PresetPluginRef("missing-builtin", "builtin"),
            ),
            available = mapOf(
                CompositionKind.UI to setOf("missing-ui"),
                CompositionKind.DEX to setOf("known-dex"),
            ),
            installed = emptyMap(),
            base = setOf("timetable"),
        )

        assertEquals(
            listOf(
                CompositionMissing("missing-builtin", CompositionKind.BUILTIN, CompositionMissingReason.NotInBase),
                CompositionMissing("known-dex", CompositionKind.DEX, CompositionMissingReason.NotInstalled),
                CompositionMissing("missing-ui", CompositionKind.UI, CompositionMissingReason.NotInstalled),
            ),
            plan.missing,
        )
    }

    @Test
    fun installedPluginMissingFromCurrentIndexIsNotReportedAsMissing() {
        val plan = resolve(
            next = preset("new", PresetPluginRef("installed-ui", "ui")),
            available = emptyMap(),
            installed = mapOf(CompositionKind.UI to setOf("installed-ui")),
        )

        assertTrue(plan.missing.isEmpty())
        assertTrue(plan.entry("installed-ui", CompositionKind.UI).enabled)
    }

    @Test
    fun unknownIndexIsNotInstalledButNotReportedAsMissingArtifact() {
        val plan = resolve(
            next = preset("new", PresetPluginRef("ghost", "ui")),
            available = emptyMap(),
            installed = emptyMap(),
        )

        assertEquals(
            listOf(CompositionMissing("ghost", CompositionKind.UI, CompositionMissingReason.NotInIndex)),
            plan.missing,
        )
    }

    @Test
    fun requiredNotInstalledIsNotBlockingButRequiredUnknownIndexIs() {
        val installable = resolve(
            next = preset("new", PresetPluginRef("ui", "ui", required = true)),
            available = mapOf(CompositionKind.UI to setOf("ui")),
            installed = emptyMap(),
        )
        assertTrue(installable.missing.any { it.reason == CompositionMissingReason.NotInstalled })
        assertTrue(installable.blockingRequiredMissing().isEmpty())

        val impossible = resolve(
            next = preset("new", PresetPluginRef("ghost", "ui", required = true)),
            available = emptyMap(),
            installed = emptyMap(),
        )
        assertEquals(
            listOf(CompositionMissing("ghost", CompositionKind.UI, CompositionMissingReason.NotInIndex)),
            impossible.blockingRequiredMissing(),
        )
    }

    @Test
    fun optionalUnknownIndexIsNotBlocking() {
        val plan = resolve(
            next = preset("new", PresetPluginRef("ghost", "ui", required = false)),
            available = emptyMap(),
            installed = emptyMap(),
        )

        assertTrue(plan.blockingRequiredMissing().isEmpty())
    }

    @Test
    fun requiredEntryDisabledByUserOrSafetyBlocksPreset() {
        val userPlan = resolve(
            next = preset("new", PresetPluginRef("ui", "ui", required = true)),
            available = mapOf(CompositionKind.UI to setOf("ui")),
            installed = mapOf(CompositionKind.UI to setOf("ui")),
            userDisabled = mapOf(CompositionKind.UI to setOf("ui")),
        )
        assertEquals(1, userPlan.blockingRequiredDisabled().size)

        val safePlan = resolve(
            next = preset("new", PresetPluginRef("ui", "ui", required = true)),
            available = mapOf(CompositionKind.UI to setOf("ui")),
            installed = mapOf(CompositionKind.UI to setOf("ui")),
            killSwitches = setOf(CompositionTarget(CompositionKind.UI, "ui")),
        )
        assertEquals(CompositionSource.SAFETY, safePlan.blockingRequiredDisabled().single().source)
    }

    @Test
    fun optionalEntryDisabledByUserDoesNotBlockPreset() {
        val plan = resolve(
            next = preset("new", PresetPluginRef("ui", "ui", required = false)),
            available = mapOf(CompositionKind.UI to setOf("ui")),
            installed = mapOf(CompositionKind.UI to setOf("ui")),
            userDisabled = mapOf(CompositionKind.UI to setOf("ui")),
        )

        assertTrue(plan.blockingRequiredDisabled().isEmpty())
    }

    @Test
    fun userDisabledBaseEntryNeverGetsReEnabledByPreset() {
        val plan = resolve(
            base = setOf("timetable"),
            userDisabled = mapOf(CompositionKind.BUILTIN to setOf("timetable")),
        )

        val entry = plan.entry("timetable", CompositionKind.BUILTIN)
        assertFalse(entry.enabled)
        assertEquals(CompositionSource.USER, entry.source)
        assertFalse(plan.forPresetApplication().entry("timetable", CompositionKind.BUILTIN).enabled)
    }

    @Test
    fun presetApplicationPlanExcludesBaseOnlyEntries() {
        val plan = resolve(
            base = setOf("timetable"),
            next = preset("new", PresetPluginRef("ui", "ui")),
            available = mapOf(CompositionKind.UI to setOf("ui")),
        )

        assertEquals(listOf("timetable", "ui"), plan.entries.map { it.id })
        assertEquals(listOf("ui"), plan.forPresetApplication().entries.map { it.id })
    }

    @Test
    fun dumpListsSourcesAndMissingReasons() {
        val plan = resolve(
            base = setOf("timetable"),
            next = preset("new", PresetPluginRef("ghost", "ui")),
        )

        val text = plan.dump()
        assertTrue(text.contains("builtin timetable enabled=true source=base"))
        assertTrue(text.contains("ui ghost enabled=true source=preset"))
        assertTrue(text.contains("reason=NotInIndex"))
    }
}
