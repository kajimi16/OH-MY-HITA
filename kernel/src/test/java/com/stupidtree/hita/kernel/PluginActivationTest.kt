package com.stupidtree.hita.kernel

import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.ServiceKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PluginActivationTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun newHost(): PluginHost = PluginHost(tmp.newFolder("plugins"))

    private class TestPlugin(
        override val id: String,
        val body: (PluginContext) -> Unit = {},
    ) : Plugin {
        override fun install(ctx: PluginContext) = body(ctx)
    }

    private fun descriptor(
        id: String,
        requires: Set<String> = emptySet(),
        provides: Set<String> = emptySet(),
    ) = PluginDescriptor(id, requires, provides)

    @Test
    fun providerInstalledLaterActivatesWaitingConsumer() {
        val host = newHost()
        val service = ServiceKey<String>("svc")
        var seen: String? = null

        host.install(
            TestPlugin("consumer") { ctx -> seen = ctx.service(service) },
            descriptor("consumer", requires = setOf("svc")),
        )

        assertFalse(host.isInstalled("consumer"))
        assertEquals(
            listOf(PluginWaiting("consumer", listOf("svc"))),
            host.diagnostics().waiting,
        )

        host.install(
            TestPlugin("provider") { ctx -> ctx.provide(service, "ok") },
            descriptor("provider", provides = setOf("svc")),
        )

        assertTrue(host.isInstalled("provider"))
        assertTrue(host.isInstalled("consumer"))
        assertEquals("ok", seen)
        assertTrue(host.diagnostics().waiting.isEmpty())
    }

    @Test
    fun cancellingWaitingPluginPreventsLaterActivation() {
        val host = newHost()
        val service = ServiceKey<String>("svc")
        var installed = false

        host.install(
            TestPlugin("consumer") { installed = true },
            descriptor("consumer", requires = setOf("svc")),
        )
        assertTrue(host.isWaiting("consumer"))

        host.uninstall("consumer")
        assertFalse(host.isWaiting("consumer"))
        assertTrue(host.diagnostics().waiting.isEmpty())

        host.install(
            TestPlugin("provider"),
            descriptor("provider", provides = setOf("svc")),
        )

        assertFalse(installed)
        assertFalse(host.isInstalled("consumer"))
    }

    @Test
    fun missingProviderStaysWaitingWithoutBecomingAFailure() {
        val host = newHost()

        host.install(
            TestPlugin("consumer"),
            descriptor("consumer", requires = setOf("not.there")),
        )

        val diagnostics = host.diagnostics()
        assertEquals(listOf("consumer"), diagnostics.waiting.map { it.id })
        assertEquals(listOf("not.there"), diagnostics.waiting.single().missing)
        assertTrue(diagnostics.failed.isEmpty())
        assertFalse(host.isInstalled("consumer"))
    }

    @Test
    fun firstPluginProviderWinsAndLaterConflictIsRecorded() {
        val host = newHost()
        val service = ServiceKey<String>("svc")

        host.install(
            TestPlugin("first") { ctx -> ctx.provide(service, "first") },
            descriptor("first", provides = setOf("svc")),
        )
        host.install(
            TestPlugin("second") { ctx -> ctx.provide(service, "second") },
            descriptor("second", provides = setOf("svc")),
        )

        assertEquals("first", host.service(service))
        assertEquals(
            listOf(PluginConflict("svc", "first", "second")),
            host.diagnostics().conflicts,
        )
        assertTrue(host.isInstalled("second"))
    }

    @Test
    fun hostGlobalServiceCannotBeOverriddenByAPlugin() {
        val host = newHost()
        val service = ServiceKey<String>("svc")
        host.provideGlobal(service, "host")

        host.install(
            TestPlugin("plugin") { ctx -> ctx.provide(service, "plugin") },
            descriptor("plugin", provides = setOf("svc")),
        )

        assertEquals("host", host.service(service))
        assertEquals(
            listOf(PluginConflict("svc", PluginHost.HOST_PROVIDER, "plugin")),
            host.diagnostics().conflicts,
        )
    }

    @Test
    fun uninstallingProviderLeavesConsumerInstalledButDegraded() {
        val host = newHost()
        val service = ServiceKey<String>("svc")

        host.install(
            TestPlugin("provider") { ctx -> ctx.provide(service, "ok") },
            descriptor("provider", provides = setOf("svc")),
        )
        host.install(
            TestPlugin("consumer") { ctx -> ctx.service(service) },
            descriptor("consumer", requires = setOf("svc")),
        )

        host.uninstall("provider")

        assertTrue(host.isInstalled("consumer"))
        assertNull(host.service(service))
        assertEquals(
            listOf(PluginDegraded("consumer", listOf("svc"))),
            host.diagnostics().degraded,
        )
    }

    @Test
    fun rejectedProviderDeclarationDoesNotCountAsAvailableAfterRealProviderLeaves() {
        val host = newHost()
        val service = ServiceKey<String>("svc")

        host.install(
            TestPlugin("real") { ctx -> ctx.provide(service, "real") },
            descriptor("real", provides = setOf("svc")),
        )
        host.install(
            TestPlugin("conflicted") { ctx -> ctx.provide(service, "conflicted") },
            descriptor("conflicted", provides = setOf("svc")),
        )
        host.install(
            TestPlugin("consumer") { ctx -> ctx.service(service) },
            descriptor("consumer", requires = setOf("svc")),
        )

        host.uninstall("real")
        host.install(
            TestPlugin("late") { ctx -> ctx.service(service) },
            descriptor("late", requires = setOf("svc")),
        )

        assertFalse(host.isInstalled("late"))
        assertEquals(listOf("late"), host.diagnostics().waiting.map { it.id })
        assertEquals(
            listOf(PluginDegraded("consumer", listOf("svc"))),
            host.diagnostics().degraded,
        )
    }

    @Test
    fun installAllResolvesWaitingPluginsInTheSameBatch() {
        val host = newHost()
        val service = ServiceKey<String>("svc")
        var seen: String? = null

        host.installAllWithDescriptors(
            listOf(
                PluginInstallation(
                    TestPlugin("consumer") { ctx -> seen = ctx.service(service) },
                    descriptor("consumer", requires = setOf("svc")),
                ),
                PluginInstallation(
                    TestPlugin("provider") { ctx -> ctx.provide(service, "ok") },
                    descriptor("provider", provides = setOf("svc")),
                ),
            ),
        )

        assertTrue(host.isInstalled("consumer"))
        assertTrue(host.isInstalled("provider"))
        assertEquals("ok", seen)
    }
}
