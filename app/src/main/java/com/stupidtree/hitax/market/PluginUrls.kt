package com.stupidtree.hitax.market

import java.io.File
import java.net.URI

/**
 * 把索引里的插件地址解析成可直接下载/读取的绝对地址。
 *
 * 索引可能在远端（HTTPS），也可能在本地私有目录，所以相对地址的基准是「索引所在位置」。
 */
object PluginUrls {

    fun isRemote(url: String): Boolean =
        url.startsWith("http://") || url.startsWith("https://")

    fun resolve(rawUrl: String, indexBase: String?): String {
        if (isRemote(rawUrl)) return rawUrl
        require(!indexBase.isNullOrBlank()) { "缺少索引位置，无法解析插件地址：$rawUrl" }
        return if (isRemote(indexBase)) {
            URI(indexBase).resolve(rawUrl).toString()
        } else {
            File(indexBase, rawUrl).absolutePath
        }
    }
}
