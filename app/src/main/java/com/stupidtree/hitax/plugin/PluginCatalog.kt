package com.stupidtree.hitax.plugin

import android.app.Application
import android.os.Build
import android.util.Log
import com.stupidtree.hita.api.HITA_API_VERSION
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.kernel.CompatibilityResult
import com.stupidtree.hita.kernel.PluginCompatibility
import com.stupidtree.hita.kernel.PluginFailurePhase
import com.stupidtree.hita.kernel.PluginInstallation
import com.stupidtree.hita.kernel.PluginRejectedException
import com.stupidtree.hitax.BuildConfig
import com.stupidtree.hitax.market.CompositionKind
import com.stupidtree.hitax.market.CompositionStore
import com.stupidtree.hitax.market.CompositionTarget
import com.stupidtree.hitax.HApplication

/**
 * 构建期插件清单 —— profile 的雏形。
 *
 * 只读 `assets/plugins.json` 里的类名并反射实例化，**不 import 任何插件模块**：
 * 删除 `:plugin-hita` / `:theta` 的依赖后，宿主仍能编译并空壳启动。
 *
 * 用户在插件市场里「卸载」过的内置插件不会随启动装配，但条目仍保留在清单里，
 * 便于市场展示与一键恢复。
 */
object PluginCatalog {

    /** 清单里的全部条目（含已被用户卸载的）。 */
    fun entries(application: Application): List<PluginEntry> = readEntries(application)

    fun entry(application: Application, pluginId: String): PluginEntry? =
        entries(application).firstOrNull { it.id == pluginId }

    /** 启动装配用：过滤掉用户已卸载的插件，并把 requires/provides 一并交给内核。 */
    fun installations(application: Application): List<PluginInstallation> {
        val safety = CompositionStore.killSwitches(application)
        return entries(application)
            .filterNot { PluginPrefs.isDisabled(application, it.id) }
            .filterNot { CompositionTarget(CompositionKind.BUILTIN, it.id) in safety }
            .mapNotNull { entry ->
                instantiate(entry, application)?.let { PluginInstallation(it, entry.descriptor()) }
            }
    }

    fun instantiate(entry: PluginEntry, application: Application): Plugin? {
        val meta = entry.artifactMeta()
        val result = PluginCompatibility.check(
            meta,
            hostAbi = BuildConfig.API_ABI.takeIf { it.isNotBlank() },
            sdkInt = Build.VERSION.SDK_INT,
            exempt = PluginExemptions.isExempt(
                application,
                meta.id,
                meta.version,
                HITA_API_VERSION,
            ),
        )
        if (result is CompatibilityResult.Rejected) {
            val error = PluginRejectedException(entry.id, result.reasons)
            runCatching {
                (application as HApplication).pluginHost
                    .reject(entry.id, error, PluginFailurePhase.COMPATIBILITY)
            }
            Log.e(TAG, "插件兼容性拒绝：${entry.className}", error)
            return null
        }
        return runCatching {
            val clazz = Class.forName(entry.className, true, PluginCatalog::class.java.classLoader)
            if (entry.needsApplication) {
                clazz.getConstructor(Application::class.java).newInstance(application) as Plugin
            } else {
                clazz.getConstructor().newInstance() as Plugin
            }
        }.onFailure {
            Log.e(TAG, "插件不可用（可能未打包）：${entry.className}", it)
            runCatching {
                (application as HApplication).pluginHost
                    .reject(entry.id, it, PluginFailurePhase.LOAD)
            }
        }.getOrNull()
    }

    private fun readEntries(application: Application): List<PluginEntry> = runCatching {
        val text = application.assets.open("plugins.json").bufferedReader().use { it.readText() }
        PluginEntries.parse(text)
    }.getOrElse {
        Log.e(TAG, "读取 plugins.json 失败", it)
        emptyList()
    }

    private const val TAG = "PluginCatalog"
}
