package com.stupidtree.hita.kernel

import com.stupidtree.hita.api.HookKey
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PluginHookTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private class TestPlugin(
        override val id: String,
        val body: (PluginContext) -> Unit,
    ) : Plugin {
        override fun install(ctx: PluginContext) = body(ctx)
    }

    @Test
    fun hooksRunInRegistrationOrderAndCanDelegate() {
        val host = PluginHost(tmp.newFolder("plugins"))
        val key = HookKey<Int>("counter")

        host.install(TestPlugin("first") { ctx ->
            ctx.hooks.register(key) { value, next -> next(value + 1) }
        })
        host.install(TestPlugin("second") { ctx ->
            ctx.hooks.register(key) { value, next ->
                if (value >= 2) value * 10 else next(value)
            }
        })

        assertEquals(20, host.runHook(key, 1))
    }

    @Test
    fun uninstallRemovesThePluginHook() {
        val host = PluginHost(tmp.newFolder("plugins"))
        val key = HookKey<Int>("counter")

        host.install(TestPlugin("first") { ctx ->
            ctx.hooks.register(key) { value, next -> next(value + 1) }
        })
        host.install(TestPlugin("second") { ctx ->
            ctx.hooks.register(key) { value, next ->
                if (value >= 2) value * 10 else next(value)
            }
        })

        host.uninstall("second")

        assertEquals(2, host.runHook(key, 1))
    }

    @Test
    fun throwingListenerFallsBackToOriginalAndIsDiagnosed() {
        val host = PluginHost(tmp.newFolder("plugins"))
        val key = HookKey<Int>("counter")

        host.install(TestPlugin("bad") { ctx ->
            ctx.hooks.register(key) { _, _ -> error("boom") }
        })

        assertEquals(1, host.runHook(key, 1))
        val failure = host.diagnostics().hookFailures.single()
        assertEquals("bad", failure.pluginId)
        assertEquals(0, failure.listenerIndex)
        assertTrue(failure.message.contains("boom"))
    }

    @Test
    fun throwAfterNextStillReturnsOriginal() {
        val host = PluginHost(tmp.newFolder("plugins"))
        val key = HookKey<Int>("counter")

        host.install(TestPlugin("bad") { ctx ->
            ctx.hooks.register(key) { _, next ->
                next(2)
                error("after next")
            }
        })

        assertEquals(1, host.runHook(key, 1))
        assertEquals(1, host.diagnostics().hookFailures.size)
    }

    @Test
    fun downstreamFailureDropsUpstreamTransform() {
        val host = PluginHost(tmp.newFolder("plugins"))
        val key = HookKey<Int>("counter")

        host.install(TestPlugin("outer") { ctx ->
            ctx.hooks.register(key) { _, next -> next(1) + 100 }
        })
        host.install(TestPlugin("inner") { ctx ->
            ctx.hooks.register(key) { _, _ -> error("inner failed") }
        })

        assertEquals(1, host.runHook(key, 1))
        assertEquals("inner", host.diagnostics().hookFailures.single().pluginId)
    }

    @Test
    fun nullReturnFallsBackAndIsDiagnosed() {
        val host = PluginHost(tmp.newFolder("plugins"))
        val key = HookKey<Int?>("nullable")

        host.install(TestPlugin("bad") { ctx ->
            ctx.hooks.register(key) { _, _ -> null }
        })

        assertEquals(7, host.runHook(key, 7))
        assertEquals(1, host.diagnostics().hookFailures.size)
    }

    @Test
    fun uninstallPrunesHookFailures() {
        val host = PluginHost(tmp.newFolder("plugins"))
        val key = HookKey<Int>("counter")

        host.install(TestPlugin("bad") { ctx ->
            ctx.hooks.register(key) { _, _ -> error("boom") }
        })
        host.runHook(key, 1)
        assertEquals(1, host.diagnostics().hookFailures.size)

        host.uninstall("bad")
        assertTrue(host.diagnostics().hookFailures.isEmpty())
    }

    @Test
    fun callingNextTwiceFallsBackToOriginal() {
        val host = PluginHost(tmp.newFolder("plugins"))
        val key = HookKey<Int>("counter")

        host.install(TestPlugin("bad") { ctx ->
            ctx.hooks.register(key) { _, next ->
                next(2)
                next(3)
                9
            }
        })

        assertEquals(1, host.runHook(key, 1))
        assertTrue(host.diagnostics().hookFailures.single().message.contains("more than once"))
    }

    @Test
    fun retainedContextCannotRegisterAfterUninstall() {
        val host = PluginHost(tmp.newFolder("plugins"))
        val key = HookKey<Int>("counter")
        var ctx: PluginContext? = null

        host.install(TestPlugin("p") { context ->
            ctx = context
            context.hooks.register(key) { value, next -> next(value + 1) }
        })
        host.uninstall("p")

        ctx!!.hooks.register(key) { value, next -> next(value + 10) }
        assertEquals(1, host.runHook(key, 1))
    }

    @Test
    fun failedInstallRollsBackHook() {
        val host = PluginHost(tmp.newFolder("plugins"))
        val key = HookKey<Int>("counter")

        host.install(TestPlugin("bad") { ctx ->
            ctx.hooks.register(key) { value, next -> next(value + 1) }
            error("install failed")
        })

        assertEquals(1, host.runHook(key, 1))
    }

    @Test
    fun shortCircuitSkipsLaterListeners() {
        val host = PluginHost(tmp.newFolder("plugins"))
        val key = HookKey<String>("text")

        host.install(TestPlugin("first") { ctx ->
            ctx.hooks.register(key) { _, _ -> "stopped" }
        })
        host.install(TestPlugin("second") { ctx ->
            ctx.hooks.register(key) { value, next -> next("$value!") }
        })

        assertEquals("stopped", host.runHook(key, "start"))
    }
}
