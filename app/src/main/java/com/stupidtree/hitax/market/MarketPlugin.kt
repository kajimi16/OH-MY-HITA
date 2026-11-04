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
)

data class MarketIndex(
    val plugins: List<MarketPlugin> = emptyList(),
    val uiPlugins: List<UiMarketPlugin> = emptyList(),
)
