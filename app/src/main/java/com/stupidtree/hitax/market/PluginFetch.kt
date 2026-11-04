package com.stupidtree.hitax.market

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** 把索引里的插件地址落到本地文件：远端走 HTTP，本地索引直接复制。 */
object PluginFetch {

    fun download(rawUrl: String, indexBase: String?, target: File) {
        val absolute = PluginUrls.resolve(rawUrl, indexBase)
        if (PluginUrls.isRemote(absolute)) {
            val conn = URL(absolute).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.inputStream.use { input -> target.outputStream().use { input.copyTo(it) } }
            conn.disconnect()
        } else {
            val source = File(absolute)
            if (!source.isFile) throw IllegalStateException("插件文件不存在：${source.path}")
            source.copyTo(target, overwrite = true)
        }
    }
}
