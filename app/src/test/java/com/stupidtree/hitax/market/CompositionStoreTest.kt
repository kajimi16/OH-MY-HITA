package com.stupidtree.hitax.market

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositionStoreTest {

    @Test
    fun snapshotIsParseableJsonWithDesiredActualAndResult() {
        val plan = CompositionPlan(
            entries = listOf(
                CompositionEntry(
                    id = "ui",
                    kind = CompositionKind.UI,
                    enabled = true,
                    required = true,
                    source = CompositionSource.PRESET,
                ),
            ),
            missing = listOf(
                CompositionMissing("ui", CompositionKind.UI, CompositionMissingReason.NotInstalled),
            ),
        )
        val state = CompositionState(
            installed = mapOf(CompositionKind.UI to setOf("old")),
            enabled = mapOf(CompositionKind.UI to setOf("old")),
        )
        val report = CompositionReport(
            installed = 1,
            enabled = 0,
            disabled = 1,
            skipped = 0,
            failures = emptyList(),
            optionalFailures = emptyList(),
            reloadNeeded = true,
        )

        val root = JsonParser.parseString(
            CompositionStore.snapshotJson(plan, state, report)
        ).asJsonObject

        assertTrue(root.has("desired"))
        assertTrue(root.has("missing"))
        assertTrue(root.has("actual"))
        assertTrue(root.has("result"))
        assertEquals("NotInstalled", root.getAsJsonArray("missing")[0].asJsonObject["reason"].asString)
    }
}
