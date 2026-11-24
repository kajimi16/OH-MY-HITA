package com.stupidtree.hitax.market

data class MarketVersion(
    val version: String,
    val url: String = "",
    val sha256: String = "",
    val size: Long = 0L,
    val minHostApi: Int? = null,
    val maxHostApi: Int? = null,
    val apiAbi: String? = null,
    val changelog: String? = null,
    val apiVersion: Int? = null,
    val entryClass: String? = null,
    val minSdk: Int? = null,
    val requires: Set<String>? = null,
    val provides: Set<String>? = null,
)

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
    /** 兼容的最高宿主 API；null 表示不设上限。 */
    val maxHostApi: Int? = null,
    /** `:api` ABI 指纹；null 表示旧索引未声明。 */
    val apiAbi: String? = null,
    /** 插件要求的最低 Android API；null 表示不限制。 */
    val minSdk: Int? = null,
    /** 依赖的服务名（`ServiceKey.name`）。 */
    val requires: Set<String> = emptySet(),
    /** 提供的服务名（`ServiceKey.name`）。 */
    val provides: Set<String> = emptySet(),
    /** 市场展示元数据，均可缺省。 */
    val author: String? = null,
    val tags: List<String>? = null,
    val repo: String? = null,
    val logo: String? = null,
    val updatedAt: String? = null,
    val downloads: Long? = null,
    val activeUsers: Long? = null,
    /** 同一插件的发行族；缺省等于 id。 */
    val family: String? = null,
    /** 可安装版本；缺省时只有顶层版本。 */
    val versions: List<MarketVersion>? = null,
) {
    fun versionOptions(): List<MarketVersion> =
        versions?.takeIf { it.isNotEmpty() }
            ?: listOf(
                MarketVersion(
                    version = version,
                    url = url,
                    sha256 = sha256,
                    size = size,
                    minHostApi = minHostApi,
                    maxHostApi = maxHostApi,
                    apiAbi = apiAbi,
                    changelog = changelog,
                    apiVersion = apiVersion,
                    entryClass = entryClass,
                    minSdk = minSdk,
                    requires = requires,
                    provides = provides,
                ),
            )

    fun forVersion(selected: MarketVersion): MarketPlugin = copy(
        version = selected.version,
        url = selected.url,
        sha256 = selected.sha256,
        size = selected.size,
        minHostApi = selected.minHostApi,
        maxHostApi = selected.maxHostApi,
        apiAbi = selected.apiAbi,
        changelog = selected.changelog ?: changelog,
        apiVersion = selected.apiVersion ?: apiVersion,
        entryClass = selected.entryClass ?: entryClass,
        minSdk = selected.minSdk ?: minSdk,
        requires = selected.requires ?: requires ?: emptySet(),
        provides = selected.provides ?: provides ?: emptySet(),
    )
}

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
    val downloads: Long? = null,
    val activeUsers: Long? = null,
    val family: String? = null,
    val versions: List<MarketVersion>? = null,
) {
    fun versionOptions(): List<MarketVersion> =
        versions?.takeIf { it.isNotEmpty() }
            ?: listOf(
                MarketVersion(
                    version = version,
                    url = url,
                    sha256 = sha256,
                    size = size,
                    minHostApi = minHostApi,
                    changelog = changelog,
                ),
            )

    fun forVersion(selected: MarketVersion): UiMarketPlugin = copy(
        version = selected.version,
        url = selected.url,
        sha256 = selected.sha256,
        size = selected.size,
        minHostApi = selected.minHostApi,
        changelog = selected.changelog ?: changelog,
    )
}

/** 服务端聚合的插件人数。 */
data class MarketStat(val downloads: Long = 0, val activeUsers: Long = 0)

/** 整合包里的一个插件引用；kind 为 ui/dex/builtin。 */
data class PresetPluginRef(
    val id: String,
    val kind: String? = null,
    val required: Boolean? = null,
) {
    fun normalizedKind(): String = kind?.lowercase() ?: "ui"
    fun isRequired(): Boolean = required != false
}

/**
 * 预设声明的一个主界面 tab。
 *
 * `id` 的解析优先级：`hita.more`（设置/入口聚合页）→ 内置 tab（如 `hita.timeline`）
 * → 运行时界面插件 id（如 `com.stupidtree.hita.ui.homework`）→ 插件注册的 TabContribution。
 */
data class PresetTab(
    val id: String = "",
    val title: String? = null,
    /** 历史字段，保留解析兼容；宿主按 id 决定来源。 */
    val kind: String? = null,
    val order: Int = 0,
    /**
     * 底栏图标。
     *
     * - `builtin:<name>`：使用宿主内置矢量图，`<name>` 见 [BuiltinTabIcons.available]；
     * - 其他字符串：APK 插件内的图标资源路径，与 plugin.json 的 mainTabs 一致。
     *
     * 解析失败（名字未知或资源缺失）时宿主回退到插件自带图标/通用图标。
     */
    val iconAsset: String? = null,
    /** 仅影响底栏可见性；required 为 true 时不可被隐藏。 */
    val visible: Boolean? = null,
    val required: Boolean? = null,
)

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
    /** 该整合包的 UI Shell 标识；为空时使用宿主默认壳。 */
    val shell: String? = null,
    /** 原项目仓库地址。 */
    val repo: String? = null,
    /**
     * 主界面底栏布局；为空时宿主退回内置的“4 个基页 + 插件 tab”行为。
     * 复刻型预设（如 hita-android-3.1.2）用它把底栏固定成与上游一致的 6 个位置。
     */
    val tabs: List<PresetTab>? = null,
    /** 支持的数据迁移，例如 timetable.ics。 */
    val migration: List<String>? = null,
    val tags: List<String>? = null,
    val author: String? = null,
    /** 同一发行版的多个版本共用 family；缺省等于 id。 */
    val family: String? = null,
) {
    fun refs(): List<PresetPluginRef> = plugins.orEmpty()
    fun familyId(): String = family?.takeIf { it.isNotBlank() } ?: id
}

data class MarketIndex(
    val plugins: List<MarketPlugin> = emptyList(),
    val uiPlugins: List<UiMarketPlugin> = emptyList(),
    val presets: List<MarketPreset>? = null,
)
