package com.stupidtree.hita.kernel

import com.stupidtree.hita.api.HITA_API_VERSION

/**
 * 加载插件之前能拿到的元数据。
 *
 * 字段与市场索引 / 内置清单 / UI 插件 `plugin.json` 对齐；
 * [apiAbi] 用于“版本号没变但契约签名变了”的破坏性变更。
 */
data class PluginArtifactMeta(
    val id: String,
    val requiresApi: Int = 1,
    val minHostApi: Int? = null,
    val maxHostApi: Int? = null,
    val apiAbi: String? = null,
    val minSdk: Int? = null,
    val version: String? = null,
)

sealed class CompatibilityResult {
    data object Compatible : CompatibilityResult()

    data class Rejected(val reasons: List<String>) : CompatibilityResult()
}

class PluginRejectedException(
    val pluginId: String,
    val reasons: List<String>,
) : IllegalStateException(
    "plugin $pluginId rejected: ${reasons.joinToString("; ")}",
)

/**
 * 纯函数兼容性检查。必须在任何 `DexClassLoader` / `Class.forName` 之前调用。
 *
 * [hostAbi] 为空时跳过 ABI 比对，兼容尚未生成 ABI 指纹的宿主；
 * [sdkInt] 默认不限制，Android 侧传入 `Build.VERSION.SDK_INT`。
 */
object PluginCompatibility {

    fun check(
        meta: PluginArtifactMeta,
        hostApi: Int = HITA_API_VERSION,
        hostAbi: String? = null,
        sdkInt: Int? = null,
        exempt: Boolean = false,
    ): CompatibilityResult {
        if (exempt) return CompatibilityResult.Compatible
        val reasons = buildList {
            val minApi = maxOf(meta.requiresApi, meta.minHostApi ?: 0)
            if (minApi > hostApi) {
                add("requires host api $minApi, host is $hostApi")
            }
            if (meta.maxHostApi != null && hostApi > meta.maxHostApi) {
                add("supports host api up to ${meta.maxHostApi}, host is $hostApi")
            }
            if (meta.apiAbi != null && hostAbi != null && meta.apiAbi != hostAbi) {
                add("api abi mismatch: plugin ${meta.apiAbi}, host $hostAbi")
            }
            if (meta.minSdk != null && sdkInt != null && sdkInt < meta.minSdk) {
                add("requires Android API ${meta.minSdk}, device is $sdkInt")
            }
        }
        return if (reasons.isEmpty()) CompatibilityResult.Compatible
        else CompatibilityResult.Rejected(reasons)
    }
}
