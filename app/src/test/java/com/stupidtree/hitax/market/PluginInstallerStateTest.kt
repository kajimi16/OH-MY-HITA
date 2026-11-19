package com.stupidtree.hitax.market

import com.stupidtree.hita.kernel.PluginLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PluginInstallerStateTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun record(id: String, sha256: String = "x") = MarketPlugin(
        id = id, name = id, version = "1", apiVersion = 2,
        url = "x.dex", sha256 = sha256, entryClass = "X",
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
    fun corruptInstalledDexIsHidden() {
        val root = tmp.newFolder("runtime-plugins")
        File(root, "corrupt").mkdirs()
        val dex = File(root, "corrupt/plugin.dex").apply { writeText("dex") }
        val sha = PluginLoader.sha256(dex)
        dex.writeText("tampered")

        val visible = PluginInstaller.filterInstalled(listOf(record("corrupt", sha)), root)

        assertTrue(visible.isEmpty())
    }

    @Test
    fun installedRecordsWithoutDexAreHidden() {
        val root = tmp.newFolder("runtime-plugins")
        File(root, "present").mkdirs()
        val dex = File(root, "present/plugin.dex").apply { writeText("dex") }

        val visible = PluginInstaller.filterInstalled(
            listOf(record("present", PluginLoader.sha256(dex)), record("gone")),
            root,
        )

        assertEquals(listOf("present"), visible.map { it.id })
    }
}

class PluginFetchTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun localIndexResolvesRelativePluginToFile() {
        val dir = tmp.newFolder("market")
        val plugin = File(dir, "ui.apk").apply { writeText("apk-bytes") }
        val target = File(tmp.root, "out.apk")

        PluginFetch.download("ui.apk", dir.absolutePath, target)

        assertEquals("apk-bytes", target.readText())
        assertTrue(plugin.isFile)
    }
}
