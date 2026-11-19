package com.stupidtree.hitax.market

import com.google.gson.Gson
import java.io.File
import java.security.MessageDigest

/** 上一次成功读取的索引，用于离线时继续展示市场列表。 */
data class CachedIndex(
    val base: String? = null,
    val raw: String = "",
    val savedAt: Long = 0L,
)

object IndexCache {
    private val gson = Gson()

    /** 远端索引的缓存目录；与市场里的命名规则保持一致。 */
    fun dirFor(filesDir: File, url: String): File {
        val name = MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
        return File(filesDir, "market-cache/$name")
    }

    /**
     * 启动期只读本地缓存，不访问网络。
     *
     * 内置源（url 为空）的索引随构建放在 `filesDir/market/index.json`；其余源读
     * [dirFor] 目录里的上次成功缓存。没有缓存返回 null。
     */
    fun loadStartup(filesDir: File, sourceUrl: String): CachedIndex? =
        if (sourceUrl.isBlank()) {
            val file = File(filesDir, "market/index.json")
            if (!file.isFile) {
                null
            } else {
                runCatching {
                    val raw = file.readText()
                    if (raw.isBlank()) null else CachedIndex(
                        base = file.parentFile?.absolutePath,
                        raw = raw,
                        savedAt = file.lastModified(),
                    )
                }.getOrNull()
            }
        } else {
            load(dirFor(filesDir, sourceUrl))
        }

    fun save(dir: File, base: String?, raw: String, now: Long = System.currentTimeMillis()) {
        runCatching {
            dir.mkdirs()
            File(dir, "index.json").writeText(
                gson.toJson(CachedIndex(base = base, raw = raw, savedAt = now))
            )
        }
    }

    fun load(dir: File): CachedIndex? = runCatching {
        val file = File(dir, "index.json")
        if (!file.isFile) return null
        gson.fromJson(file.readText(), CachedIndex::class.java)
            ?.takeIf { it.raw.isNotBlank() }
    }.getOrNull()
}
