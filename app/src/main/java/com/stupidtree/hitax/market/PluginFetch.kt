package com.stupidtree.hitax.market

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** 把索引里的插件地址落到本地文件：远端走 HTTP，本地索引直接复制。 */
object PluginFetch {

    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val TOTAL_TIMEOUT_MS = 90_000L
    private const val MAX_ATTEMPTS = 3

    fun download(
        rawUrl: String,
        indexBase: String?,
        target: File,
        totalTimeoutMs: Long = TOTAL_TIMEOUT_MS,
    ) {
        val absolute = PluginUrls.resolve(rawUrl, indexBase)
        if (PluginUrls.isRemote(absolute)) {
            var last: Throwable? = null
            val deadline = System.nanoTime() + totalTimeoutMs * 1_000_000L
            for (attempt in 0 until MAX_ATTEMPTS) {
                if (deadline <= System.nanoTime()) break
                val remainingMs = ((deadline - System.nanoTime()) / 1_000_000L)
                    .coerceAtLeast(1L)
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()
                try {
                    target.delete()
                    target.parentFile?.mkdirs()
                    val conn = URL(absolute).openConnection() as HttpURLConnection
                    conn.instanceFollowRedirects = true
                    conn.connectTimeout = minOf(CONNECT_TIMEOUT_MS, remainingMs)
                    conn.readTimeout = minOf(READ_TIMEOUT_MS, remainingMs)
                    try {
                        conn.inputStream.use { input -> target.outputStream().use { input.copyTo(it) } }
                    } finally {
                        conn.disconnect()
                    }
                    if (target.length() > 0L) return
                    last = IllegalStateException("下载为空")
                } catch (t: Throwable) {
                    last = t
                    if (attempt < MAX_ATTEMPTS - 1 && deadline > System.nanoTime()) {
                        Thread.sleep(500L)
                    }
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
