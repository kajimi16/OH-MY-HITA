package com.stupidtree.hitax

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.stupidtree.hita.kernel.PluginDiagnostics
import com.stupidtree.hita.kernel.PluginOrigin
import com.stupidtree.hita.kernel.PluginSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsExportTest {

    private fun diagnosticsWith(snapshot: PluginSnapshot) = PluginDiagnostics(
        installed = listOf(snapshot),
        failed = emptyList(),
        waiting = emptyList(),
        conflicts = emptyList(),
        degraded = emptyList(),
        services = emptyList(),
    )

    @Test
    fun writesBothExportedFilesAsParseableJson() {
        val files = pluginDiagnosticsExport(
            diagnostics = diagnosticsWith(
                PluginSnapshot(
                    id = "com.example.plugin",
                    requiresApi = 8,
                    origin = PluginOrigin.BUILTIN,
                    version = "1.0.0",
                    requires = listOf("hita.theme"),
                    provides = listOf("hita.timetable"),
                ),
            ),
            composition = null,
            exportedAt = 1_700_000_000_000L,
            appVersionName = "0.0.1",
        )

        assertEquals(
            setOf(DIAGNOSTICS_EXPORT_FILE, COMPOSITION_EXPORT_FILE),
            files.keys,
        )
        val combined = JsonParser.parseString(files.getValue(DIAGNOSTICS_EXPORT_FILE)).asJsonObject
        assertEquals(1_700_000_000_000L, combined["exportedAt"].asLong)
        assertEquals("0.0.1", combined["appVersion"].asString)
        val installed = combined.getAsJsonObject("plugins")
            .getAsJsonArray("installed")[0].asJsonObject
        assertEquals("com.example.plugin", installed["id"].asString)
        assertEquals("BUILTIN", installed["origin"].asString)
        assertEquals("1.0.0", installed["version"].asString)
        assertEquals("hita.theme", installed["requires"].asJsonArray[0].asString)
        assertEquals("hita.timetable", installed["provides"].asJsonArray[0].asString)

        // 没有组合快照时 composition.json 仍是可解析对象，且与内嵌段一致。
        val composition = JsonParser.parseString(files.getValue(COMPOSITION_EXPORT_FILE)).asJsonObject
        assertFalse(composition["present"].asBoolean)
        assertEquals(composition, combined["composition"])
    }

    @Test
    fun embedsExistingCompositionSnapshotVerbatim() {
        val composition = JsonParser.parseString(
            """{"plan":{"entries":[]},"startup":{"attempted":true}}""",
        ).asJsonObject

        val files = pluginDiagnosticsExport(
            diagnostics = diagnosticsWith(PluginSnapshot("plain", 1)),
            composition = composition,
            exportedAt = 7L,
            appVersionName = null,
        )

        val combined = JsonParser.parseString(files.getValue(DIAGNOSTICS_EXPORT_FILE)).asJsonObject
        assertTrue(combined["composition"].asJsonObject["startup"].asJsonObject["attempted"].asBoolean)
        assertEquals(
            composition,
            JsonParser.parseString(files.getValue(COMPOSITION_EXPORT_FILE)) as JsonObject,
        )
    }
}
