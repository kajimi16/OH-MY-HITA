package com.stupidtree.hita.kernel

import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.ServiceKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PluginDiagnosticsTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun newHost(): PluginHost = PluginHost(tmp.newFolder("plugins"))

    private class TestPlugin(
        override val id: String,
        val body: (PluginContext) -> Unit = {},
    ) : Plugin {
        override fun install(ctx: PluginContext) = body(ctx)
    }

    @Test
    fun installedSnapshotsCarryDescriptorMetadataInStableOrder() {
        val host = newHost()
        host.provideGlobal(ServiceKey<String>("svc.a"), "a")
        host.provideGlobal(ServiceKey<String>("svc.b"), "b")
        host.install(
            object : Plugin {
                override val id: String = "zeta"
                override val requiresApi: Int = 7
                override fun install(ctx: PluginContext) = Unit
            },
            PluginDescriptor(
                id = "zeta",
                requires = setOf("svc.b", "svc.a"),
                provides = setOf("svc.z", "svc.y"),
                origin = PluginOrigin.BUILTIN,
                version = "1.2.3",
            ),
        )
        host.install(TestPlugin("alpha"))

        val installed = host.diagnostics().installed

        assertEquals(listOf("alpha", "zeta"), installed.map { it.id })
        assertEquals(PluginSnapshot("alpha", 1), installed[0])
        assertEquals(
            PluginSnapshot(
                id = "zeta",
                requiresApi = 7,
                origin = PluginOrigin.BUILTIN,
                version = "1.2.3",
                requires = listOf("svc.a", "svc.b"),
                provides = listOf("svc.y", "svc.z"),
            ),
            installed[1],
        )
    }

    @Test
    fun snapshotFallsBackToUnknownOriginWhenNoDescriptorWasGiven() {
        val host = newHost()
        host.install(TestPlugin("plain"))

        assertEquals(
            PluginSnapshot("plain", 1, PluginOrigin.UNKNOWN, null, emptyList(), emptyList()),
            host.diagnostics().installed.single(),
        )
    }

    @Test
    fun reportsInstalledPluginsAndServiceOwnersInStableOrder() {
        val host = newHost()
        val globalKey = ServiceKey<String>("global.service")
        val pluginKey = ServiceKey<String>("plugin.service")
        host.provideGlobal(globalKey, "host")
        host.install(TestPlugin("b") { ctx -> ctx.provide(pluginKey, "plugin") })
        host.install(TestPlugin("a"))

        val diagnostics = host.diagnostics()

        assertEquals(listOf("a", "b"), diagnostics.installed.map { it.id })
        assertEquals(
            listOf(
                PluginServiceBinding("global.service", PluginHost.HOST_PROVIDER),
                PluginServiceBinding("plugin.service", "b"),
            ),
            diagnostics.services,
        )
    }

    @Test
    fun distinguishesCompatibilityRejectionFromInstallFailure() {
        val host = newHost()
        host.reject("blocked", IllegalStateException("api too new"), PluginFailurePhase.COMPATIBILITY)
        host.install(TestPlugin("broken") { error("boom") })

        val failed = host.diagnostics().failed

        assertEquals(listOf("blocked", "broken"), failed.map { it.id })
        assertEquals(PluginFailurePhase.COMPATIBILITY, failed[0].phase)
        assertEquals(PluginFailurePhase.INSTALL, failed[1].phase)
        assertEquals("boom", failed[1].message)
    }

    @Test
    fun laterProviderIsRejectedAndFirstOwnerSurvivesUninstall() {
        val host = newHost()
        val key = ServiceKey<String>("shared")
        host.install(TestPlugin("first") { ctx -> ctx.provide(key, "first") })
        host.install(TestPlugin("second") { ctx -> ctx.provide(key, "second") })

        assertEquals("first", host.diagnostics().services.single().provider)
        assertEquals(
            listOf(PluginConflict("shared", "first", "second")),
            host.diagnostics().conflicts,
        )

        host.uninstall("second")

        assertEquals("first", host.diagnostics().services.single().provider)
    }

    @Test
    fun successfulInstallClearsAnEarlierFailure() {
        val host = newHost()
        host.reject("same", IllegalStateException("first attempt"), PluginFailurePhase.LOAD)
        host.install(TestPlugin("same"))

        assertTrue(host.diagnostics().failed.isEmpty())
        assertEquals(listOf("same"), host.diagnostics().installed.map { it.id })
    }

    @Test
    fun declaredServiceThatIsActuallyProvidedIsNotReported() {
        val host = newHost()
        val key = ServiceKey<String>("svc")

        host.install(
            TestPlugin("provider") { ctx -> ctx.provide(key, "ok") },
            PluginDescriptor("provider", provides = setOf("svc")),
        )

        assertTrue(host.diagnostics().declaredProvides.isEmpty())
    }

    @Test
    fun declaredServiceWithoutProviderIsReportedButPluginStaysInstalled() {
        val host = newHost()

        host.install(
            TestPlugin("ghost"),
            PluginDescriptor("ghost", provides = setOf("b.service", "a.service")),
        )

        assertEquals(
            listOf(
                PluginDeclaredProvides("ghost", "a.service", null),
                PluginDeclaredProvides("ghost", "b.service", null),
            ),
            host.diagnostics().declaredProvides,
        )
        // 声明只是清单提示，不影响激活。
        assertTrue(host.isInstalled("ghost"))
        assertTrue(host.diagnostics().failed.isEmpty())
    }

    @Test
    fun conflictRejectedDeclarationShowsTheActualProvider() {
        val host = newHost()
        val key = ServiceKey<String>("svc")

        host.install(
            TestPlugin("first") { ctx -> ctx.provide(key, "first") },
            PluginDescriptor("first", provides = setOf("svc")),
        )
        host.install(
            TestPlugin("second") { ctx -> ctx.provide(key, "second") },
            PluginDescriptor("second", provides = setOf("svc")),
        )

        assertEquals(
            listOf(PluginDeclaredProvides("second", "svc", "first")),
            host.diagnostics().declaredProvides,
        )
    }

    @Test
    fun declaredServiceHeldByHostShowsHostAsActualProvider() {
        val host = newHost()
        val key = ServiceKey<String>("svc")
        host.provideGlobal(key, "host")

        host.install(
            TestPlugin("claimant") { ctx -> ctx.provide(key, "plugin") },
            PluginDescriptor("claimant", provides = setOf("svc")),
        )

        assertEquals(
            listOf(PluginDeclaredProvides("claimant", "svc", PluginHost.HOST_PROVIDER)),
            host.diagnostics().declaredProvides,
        )
    }
}
