package com.stupidtree.hitax.market

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CompositionMaterializerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val sha = "a".repeat(64)

    private fun uiEntry(id: String) = UiMarketPlugin(
        id = id,
        name = id,
        version = "1",
        url = "$id.apk",
        sha256 = sha,
    )

    private fun dexEntry(id: String) = MarketPlugin(
        id = id,
        name = id,
        version = "1",
        apiVersion = 2,
        url = "$id.dex",
        sha256 = sha,
        entryClass = "X",
    )

    /** 在 staging 下真的落文件，模拟 prepare 只写 staging、不碰已安装状态。 */
    private inner class FakeMaterializer(
        private val fail: Set<CompositionTarget> = emptySet(),
    ) : CompositionMaterializer {
        val stagedFiles = mutableListOf<File>()
        val prepareCalls = mutableListOf<CompositionTarget>()
        var cleanups = 0

        override fun prepare(kind: CompositionKind, id: String): Result<PreparedArtifact> {
            val target = CompositionTarget(kind, id)
            prepareCalls += target
            if (target in fail) {
                return Result.failure(IllegalStateException("download failed"))
            }
            val file = File(tmp.root, "staging/${kind.name}-$id.bin").apply {
                parentFile?.mkdirs()
                writeText("bytes")
            }
            stagedFiles += file
            return Result.success(
                when (kind) {
                    CompositionKind.UI -> PreparedArtifact.Ui(uiEntry(id), file, sha)
                    CompositionKind.DEX -> PreparedArtifact.Dex(dexEntry(id), file)
                    CompositionKind.BUILTIN -> error("BUILTIN 不应进入 materializer")
                },
            )
        }

        override fun cleanup() {
            cleanups++
            File(tmp.root, "staging").deleteRecursively()
        }
    }

    private class RecordingExecutor : CompositionExecutor {
        val calls = mutableListOf<String>()

        override fun install(kind: CompositionKind, id: String): Result<Boolean> {
            calls += "install:${kind.name}:$id"
            return Result.success(kind == CompositionKind.UI)
        }

        override fun enable(kind: CompositionKind, id: String): Result<Boolean> {
            calls += "enable:${kind.name}:$id"
            return Result.success(true)
        }

        override fun disable(kind: CompositionKind, id: String): Result<Boolean> {
            calls += "disable:${kind.name}:$id"
            return Result.success(true)
        }

        override fun restore(
            kind: CompositionKind,
            id: String,
            installed: Boolean,
            enabled: Boolean,
        ): Result<Boolean> {
            calls += "restore:${kind.name}:$id:$installed:$enabled"
            return Result.success(true)
        }
    }

    private fun entry(
        id: String,
        kind: CompositionKind,
        enabled: Boolean = true,
        required: Boolean = false,
    ) = CompositionEntry(
        id = id,
        kind = kind,
        enabled = enabled,
        required = required,
        source = CompositionSource.PRESET,
    )

    private fun state() = CompositionState(
        installed = mapOf(CompositionKind.UI to setOf("old-ui")),
        enabled = mapOf(CompositionKind.UI to setOf("old-ui")),
    )

    @Test
    fun requiredPrepareFailureDoesNotReconcileOrTouchOldEntries() {
        val executor = RecordingExecutor()
        val materializer = FakeMaterializer(fail = setOf(CompositionTarget(CompositionKind.UI, "new-ui")))

        val report = CompositionReconciler.executePrepared(
            plan = CompositionPlan(
                entries = listOf(
                    entry("new-ui", CompositionKind.UI, required = true),
                    entry("old-ui", CompositionKind.UI, enabled = false),
                ),
                missing = listOf(
                    CompositionMissing("new-ui", CompositionKind.UI, CompositionMissingReason.NotInstalled),
                ),
            ),
            state = state(),
            materializer = materializer,
            executor = executor,
        )

        // 没有任何 executor 调用 = 旧条目没被停用、active preset 保持原样。
        assertTrue(executor.calls.isEmpty())
        assertEquals(1, report.failures.size)
        assertTrue(report.failures.single().contains("download failed"))
        assertFalse(report.canActivate)
        assertEquals(0, report.disabled)
        assertEquals(2, report.skipped)
        assertEquals(1, materializer.cleanups)
        assertFalse(File(tmp.root, "staging").exists())
    }

    @Test
    fun optionalPrepareFailureStillReconcilesRequiredEntries() {
        val executor = RecordingExecutor()
        val materializer = FakeMaterializer(fail = setOf(CompositionTarget(CompositionKind.UI, "opt-ui")))

        val report = CompositionReconciler.executePrepared(
            plan = CompositionPlan(
                entries = listOf(
                    entry("required-ui", CompositionKind.UI, required = true),
                    entry("opt-ui", CompositionKind.UI),
                    entry("old-ui", CompositionKind.UI, enabled = false),
                ),
                missing = listOf(
                    CompositionMissing(
                        "required-ui",
                        CompositionKind.UI,
                        CompositionMissingReason.NotInstalled,
                    ),
                    CompositionMissing("opt-ui", CompositionKind.UI, CompositionMissingReason.NotInstalled),
                ),
            ),
            state = state(),
            materializer = materializer,
            executor = executor,
        )

        assertEquals(
            listOf("install:UI:required-ui", "disable:UI:old-ui"),
            executor.calls,
        )
        assertEquals(1, report.installed)
        assertEquals(1, report.disabled)
        assertEquals(1, report.optionalFailures.size)
        assertTrue(report.optionalFailures.single().contains("download failed"))
        assertTrue(report.failures.isEmpty())
        assertTrue(report.canActivate)
        assertEquals(1, materializer.cleanups)
        assertFalse(File(tmp.root, "staging").exists())
    }

    @Test
    fun successPathCleansStaging() {
        val executor = RecordingExecutor()
        val materializer = FakeMaterializer()

        val report = CompositionReconciler.executePrepared(
            plan = CompositionPlan(
                entries = listOf(entry("new-ui", CompositionKind.UI, required = true)),
                missing = listOf(
                    CompositionMissing("new-ui", CompositionKind.UI, CompositionMissingReason.NotInstalled),
                ),
            ),
            state = CompositionState(),
            materializer = materializer,
            executor = executor,
        )

        assertEquals(listOf("install:UI:new-ui"), executor.calls)
        assertTrue(report.canActivate)
        assertEquals(1, report.installed)
        assertEquals(1, materializer.stagedFiles.size)
        assertEquals(1, materializer.cleanups)
        assertFalse(File(tmp.root, "staging").exists())
    }

    @Test
    fun cleanupRunsWhenExecutorThrows() {
        val materializer = FakeMaterializer()
        val executor = RecordingExecutor()

        assertThrows(IllegalStateException::class.java) {
            CompositionReconciler.executePrepared(
                plan = CompositionPlan(
                    entries = listOf(entry("new-ui", CompositionKind.UI, required = true)),
                    missing = listOf(
                        CompositionMissing(
                            "new-ui",
                            CompositionKind.UI,
                            CompositionMissingReason.NotInstalled,
                        ),
                    ),
                ),
                state = CompositionState(),
                materializer = materializer,
                executor = object : CompositionExecutor {
                    override fun install(kind: CompositionKind, id: String): Result<Boolean> =
                        throw IllegalStateException("boom")

                    override fun enable(kind: CompositionKind, id: String): Result<Boolean> =
                        executor.enable(kind, id)

                    override fun disable(kind: CompositionKind, id: String): Result<Boolean> =
                        executor.disable(kind, id)

                    override fun restore(
                        kind: CompositionKind,
                        id: String,
                        installed: Boolean,
                        enabled: Boolean,
                    ): Result<Boolean> = executor.restore(kind, id, installed, enabled)
                },
            )
        }

        assertEquals(1, materializer.cleanups)
        assertFalse(File(tmp.root, "staging").exists())
    }

    @Test
    fun builtinInstallNeverUsesMaterializer() {
        val executor = RecordingExecutor()
        val materializer = FakeMaterializer()

        val report = CompositionReconciler.executePrepared(
            plan = CompositionPlan(
                entries = listOf(entry("builtin-x", CompositionKind.BUILTIN, required = true)),
                missing = listOf(
                    CompositionMissing(
                        "builtin-x",
                        CompositionKind.BUILTIN,
                        CompositionMissingReason.NotInstalled,
                    ),
                ),
            ),
            state = CompositionState(),
            materializer = materializer,
            executor = executor,
        )

        assertTrue(materializer.prepareCalls.isEmpty())
        assertEquals(listOf("install:BUILTIN:builtin-x"), executor.calls)
        assertTrue(report.canActivate)
        assertEquals(1, materializer.cleanups)
    }
}
