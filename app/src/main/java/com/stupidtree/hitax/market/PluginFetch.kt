package com.stupidtree.hitax.market

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** 把索引里的插件地址落到本地文件：远端走 HTTP，本地索引直接复制。 */
object PluginFetch {

    fun download(rawUrl: String, indexBase: String?, target: File) {
        val absolute = PluginUrls.resolve(rawUrl, indexBase)
        if (PluginUrls.isRemote(absolute)) {
            var last: Throwable? = null
            repeat(3) { attempt ->
                try {
                    target.delete()
                    val conn = URL(absolute).openConnection() as HttpURLConnection
                    conn.instanceFollowRedirects = true
                    conn.connectTimeout = 30_000
                    conn.readTimeout = 120_000
                    try {
                        conn.inputStream.use { input -> target.outputStream().use { input.copyTo(it) } }
                    } finally {
                        conn.disconnect()
                    }
                    if (target.length() > 0L) return
                    last = IllegalStateException("下载为空")
                } catch (t: Throwable) {
                    last = t
                    if (attempt < 2) Thread.sleep(1_000L)
                }
            }
            target.delete()
            throw IllegalStateException("下载失败：${absolute}：${last?.message}", last)
        } else {
            val source = File(absolute)
            if (!source.isFile) throw IllegalStateException("插件文件不存在：${source.path}")
            source.copyTo(target, overwrite = true)
        }
    }
}
