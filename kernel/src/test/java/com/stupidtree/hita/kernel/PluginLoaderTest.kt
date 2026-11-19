package com.stupidtree.hita.kernel

import com.stupidtree.hita.api.HITA_API_VERSION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PluginLoaderTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun sha256MatchesKnownDigest() {
        val file = tmp.newFile("plugin.dex").apply { writeText("hello") }
        // sha256("hello")
        assertEquals(
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
            PluginLoader.sha256(file),
        )
    }

    @Test
    fun sha256ChangesWhenContentChanges() {
        val a = tmp.newFile("a.dex").apply { writeText("v1") }
        val b = tmp.newFile("b.dex").apply { writeText("v2") }
        assert(PluginLoader.sha256(a) != PluginLoader.sha256(b))
    }

    @Test
    fun incompatibleMetaIsRejectedBeforeTheDexFileIsTouched() {
        val host = PluginHost(tmp.newFolder("plugins"))
        val loader = PluginLoader(host)
        val missingDex = tmp.newFile("missing.dex").apply { delete() }

        val error = assertThrows(PluginRejectedException::class.java) {
            loader.loadAndInstall(
                dexFile = missingDex,
                entryClassName = "example.Plugin",
                optimizedDir = tmp.newFolder("opt"),
                meta = PluginArtifactMeta("future", requiresApi = HITA_API_VERSION + 1),
            )
        }

        assertTrue(error.reasons.single().contains("requires host api"))
        val failure = host.diagnostics().failed.single()
        assertEquals("future", failure.id)
        assertEquals(PluginFailurePhase.COMPATIBILITY, failure.phase)
    }

    @Test
    fun minSdkIsRejectedBeforeTheDexFileIsTouched() {
        val host = PluginHost(tmp.newFolder("plugins"))
        val loader = PluginLoader(host)
        val missingDex = tmp.newFile("missing-sdk.dex").apply { delete() }

        val error = assertThrows(PluginRejectedException::class.java) {
            loader.loadAndInstall(
                dexFile = missingDex,
                entryClassName = "example.Plugin",
                optimizedDir = tmp.newFolder("opt-sdk"),
                meta = PluginArtifactMeta("old-device", minSdk = 99),
                sdkInt = 30,
            )
        }

        assertTrue(error.reasons.single().contains("requires Android API 99"))
        assertEquals(PluginFailurePhase.COMPATIBILITY, host.diagnostics().failed.single().phase)
    }

    @Test
    fun compatibleMetaStillRequiresARealDexFile() {
        val loader = PluginLoader(PluginHost(tmp.newFolder("plugins2")))
        val missingDex = tmp.newFile("missing2.dex").apply { delete() }

        assertThrows(IllegalArgumentException::class.java) {
            loader.loadAndInstall(
                dexFile = missingDex,
                entryClassName = "example.Plugin",
                optimizedDir = tmp.newFolder("opt2"),
                meta = PluginArtifactMeta("ok", requiresApi = HITA_API_VERSION),
            )
        }
    }
}
