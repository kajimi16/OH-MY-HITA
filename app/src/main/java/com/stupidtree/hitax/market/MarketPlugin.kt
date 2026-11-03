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
)

data class MarketIndex(
    val plugins: List<MarketPlugin> = emptyList(),
)
