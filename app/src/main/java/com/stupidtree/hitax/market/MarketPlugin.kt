package com.stupidtree.hitax.market

/**
 * 市场索引里的一条插件记录。
 *
 * 索引是可信源：它给出的 `sha256` 就是完整性校验依据。
 * 将来接 HTTPS 签名索引时，只需要替换索引的获取方式，安装流程不变。
 */
data class MarketPlugin(
    val id: String,
    val name: String,
    val version: String,
    val apiVersion: Int,
    val url: String,
    val sha256: String,
    val entryClass: String,
    /** 市场详情：可选字段，旧索引缺省为 null。 */
    val description: String? = null,
    val changelog: String? = null,
    /** APK/dex 体积（字节），0 表示未知。 */
    val size: Long = 0L,
    /** 安装所需的最低宿主 API；null 表示旧索引未声明，按 1 处理。 */
    val minHostApi: Int? = null,
    /** 市场展示元数据，均可缺省。 */
    val author: String? = null,
    val tags: List<String>? = null,
    val repo: String? = null,
    val logo: String? = null,
    val updatedAt: String? = null,
)

/** 市场里的运行时界面插件（独立 APK + Compose 容器）。 */
data class UiMarketPlugin(
    val id: String,
    val name: String,
    val version: String,
    /** APK 地址，可为相对索引的路径。 */
    val url: String,
    val sha256: String,
    val description: String? = null,
    val changelog: String? = null,
    val size: Long = 0L,
    val minHostApi: Int? = null,
    val author: String? = null,
    val tags: List<String>? = null,
    val repo: String? = null,
    val logo: String? = null,
    val updatedAt: String? = null,
)

/** 整合包里的一个插件引用；kind 为 ui/dex/builtin。 */
data class PresetPluginRef(
    val id: String,
    val kind: String? = null,
    val required: Boolean? = null,
) {
    fun normalizedKind(): String = kind?.lowercase() ?: "ui"
    fun isRequired(): Boolean = required != false
}

/** 一站式预设：引用一组市场插件，不打包插件源码。 */
data class MarketPreset(
    val id: String,
    val name: String,
    val version: String = "1",
    /** 复刻来源，例如 HITA_Android v3.1.2。 */
    val source: String? = null,
    val description: String? = null,
    val minHostApi: Int? = null,
    val plugins: List<PresetPluginRef>? = null,
    /** 原项目仓库地址。 */
    val repo: String? = null,
    /** 支持的数据迁移，例如 timetable.ics。 */
    val migration: List<String>? = null,
    val tags: List<String>? = null,
    val author: String? = null,
) {
    fun refs(): List<PresetPluginRef> = plugins.orEmpty()
}

data class MarketIndex(
    val plugins: List<MarketPlugin> = emptyList(),
    val uiPlugins: List<UiMarketPlugin> = emptyList(),
    val presets: List<MarketPreset>? = null,
)
