package com.stupidtree.hita.kernel

import com.stupidtree.hita.api.HITA_API_VERSION
import com.stupidtree.hita.api.NavEntry
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.SearchTab
import com.stupidtree.hita.api.TextNavEntry
import com.stupidtree.hita.api.ViewSearchTab
import com.stupidtree.hita.api.ServiceKey
import com.stupidtree.hita.api.SyncCodec
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PluginHostTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val key = ServiceKey<Any>("test.service")

    private fun newHost(logs: MutableList<String> = mutableListOf()): PluginHost =
        PluginHost(tmp.newFolder("plugins"), { message, _ -> logs += message })

    private class TestPlugin(
        override val id: String,
        override val requiresApi: Int = HITA_API_VERSION,
        private val body: (PluginContext) -> Unit,
    ) : Plugin {
        var installs = 0
        var purges = 0
        override fun install(ctx: PluginContext) {
            installs++
            body(ctx)
        }

        override fun purgeData() {
            purges++
        }
    }

    private class TestCodec(override val key: String) : SyncCodec {
        override fun export(ids: List<String>): List<JsonObject> = emptyList()
        override fun save(ids: List<String>, data: List<String>) = Unit
        override fun delete(ids: List<String>) = Unit
        override fun clear() = Unit
    }

    @Test
    fun installRegistersEveryContributionPoint() {
        val host = newHost()
        host.install(
            TestPlugin("p1") { ctx ->
                ctx.provide(key, "service-impl")
                ctx.nav.register(NavEntry("nav.1", 0, 0, order = 10) { null })
                ctx.searchTabs.register(SearchTab("tab.1", 0, 10) { error("unused") })
                ctx.sync.register(TestCodec("timetable"))
            }
        )

        assertEquals("service-impl", host.service(key))
        assertEquals(listOf("nav.1"), host.nav.entries().map { it.id })
        assertEquals(listOf("tab.1"), host.searchTabs.entries().map { it.id })
        assertEquals(setOf("timetable"), host.sync.keys())
        assertEquals(listOf("p1"), host.installedIds())
        assertTrue(host.failures().isEmpty())
    }

    @Test
    fun failedInstallRollsBackEveryRegistration() {
        val host = newHost()
        host.install(
            TestPlugin("broken") { ctx ->
                ctx.provide(key, "leaked")
                ctx.nav.register(NavEntry("nav.broken", 0, 0) { null })
                ctx.nav.registerText(TextNavEntry("nav.text", "text", 0) { null })
                ctx.searchTabs.register(SearchTab("tab.broken", 0, 0) { error("unused") })
                ctx.searchTabs.registerView(
                    ViewSearchTab("view.broken", "broken", 0, { error("unused") })
                )
                ctx.sync.register(TestCodec("leaked-table"))
                throw IllegalStateException("boom")
            }
        )

        assertNull(host.service(key))
        assertTrue(host.nav.entries().isEmpty())
        assertTrue(host.nav.textEntries().isEmpty())
        assertTrue(host.searchTabs.entries().isEmpty())
        assertTrue(host.searchTabs.viewEntries().isEmpty())
        assertTrue(host.sync.keys().isEmpty())
        assertTrue(host.installedIds().isEmpty())
        assertNotNull(host.failures()["broken"])
    }

    @Test
    fun uninstallRestoresPreviousProvider() {
        val host = newHost()
        host.install(TestPlugin("first") { ctx -> ctx.provide(key, "first-impl") })
        host.install(TestPlugin("second") { ctx -> ctx.provide(key, "second-impl") })

        assertEquals("second-impl", host.service(key))

        host.uninstall("second")
        assertEquals("first-impl", host.service(key))

        host.uninstall("first")
        assertNull(host.service(key))
    }

    @Test
    fun samePluginIdIsInstalledOnlyOnce() {
        val host = newHost()
        val plugin = TestPlugin("dup") { }
        host.install(plugin)
        host.install(plugin)

        assertEquals(1, plugin.installs)
        assertEquals(listOf("dup"), host.installedIds())
    }

    @Test
    fun pluginRequiringNewerApiIsRejected() {
        val host = newHost()
        host.install(TestPlugin("future", requiresApi = HITA_API_VERSION + 1) { })

        assertTrue(host.installedIds().isEmpty())
        assertNotNull(host.failures()["future"])
    }

    @Test
    fun globalServiceIsVisibleToPluginsAndSurvivesUninstall() {
        val host = newHost()
        host.provideGlobal(key, "host-service")

        var seen: Any? = null
        host.install(TestPlugin("p") { ctx -> seen = ctx.service(key) })
        assertEquals("host-service", seen)

        host.uninstall("p")
        assertEquals("host-service", host.service(key))
    }

    @Test
    fun uninstallHookStillSeesServicesBeforeRollback() {
        val host = newHost()
        val key2 = ServiceKey<String>("plugin.own")
        var seenInUninstall: String? = null

        val plugin = object : Plugin {
            override val id = "with-hook"
            override fun install(ctx: PluginContext) {
                ctx.provide(key2, "own")
            }

            override fun uninstall(ctx: PluginContext) {
                seenInUninstall = ctx.service(key2)
            }
        }

        host.install(plugin)
        host.uninstall("with-hook")

        assertEquals("own", seenInUninstall)
        assertNull(host.service(key2))
    }

    @Test
    fun uninstallWithPurgeClearsRegistrationsAndCallsPurge() {
        val host = newHost()
        val plugin = TestPlugin("p-purge") { ctx -> ctx.provide(key, "impl") }
        host.install(plugin)

        host.uninstall("p-purge", purgeData = true)

        assertEquals(1, plugin.purges)
        assertNull(host.service(key))
        assertTrue(!host.isInstalled("p-purge"))
        assertNull(host.plugin("p-purge"))
    }

    @Test
    fun uninstallWithoutPurgeKeepsPluginOwnedData() {
        val host = newHost()
        val plugin = TestPlugin("p-keep") { ctx -> ctx.provide(key, "impl") }
        host.install(plugin)

        host.uninstall("p-keep")

        assertEquals(0, plugin.purges)
        assertNull(host.service(key))
    }

    @Test
    fun eachPluginGetsItsOwnDirectory() {
        val host = newHost()
        var dirA: java.io.File? = null
        var dirB: java.io.File? = null
        host.install(TestPlugin("a") { dirA = it.filesDir() })
        host.install(TestPlugin("b") { dirB = it.filesDir() })

        assertEquals("a", dirA?.name)
        assertEquals("b", dirB?.name)
        assertTrue(dirA!!.isDirectory)
        assertTrue(dirA != dirB)
    }
}
