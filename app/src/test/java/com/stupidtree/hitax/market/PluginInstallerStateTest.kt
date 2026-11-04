package com.stupidtree.hitax.market

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PluginInstallerStateTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun record(id: String) = MarketPlugin(
        id = id, name = id, version = "1", apiVersion = 2,
        url = "x.dex", sha256 = "x", entryClass = "X",
    )

    @Test
    fun missingDisabledFileMeansEverythingEnabled() {
        assertTrue(PluginInstaller.parseDisabled(null).isEmpty())
        assertTrue(PluginInstaller.parseDisabled("  ").isEmpty())
    }

    @Test
    fun disabledIdsAreReadAsSet() {
        assertEquals(setOf("a", "b"), PluginInstaller.parseDisabled("""["a","b"]"""))
    }

    @Test
    fun corruptDisabledFileDoesNotCrash() {
        assertTrue(PluginInstaller.parseDisabled("not json").isEmpty())
    }

    @Test
    fun installedRecordsWithoutDexAreHidden() {
        val root = tmp.newFolder("runtime-plugins")
        File(root, "present").mkdirs()
        File(root, "present/plugin.dex").writeText("dex")

        val visible = PluginInstaller.filterInstalled(listOf(record("present"), record("gone")), root)

        assertEquals(listOf("present"), visible.map { it.id })
    }
}
