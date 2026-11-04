package com.stupidtree.hitax.market

import com.google.gson.Gson
import java.io.File

/** 上一次成功读取的索引，用于离线时继续展示市场列表。 */
data class CachedIndex(
    val base: String? = null,
    val raw: String = "",
    val savedAt: Long = 0L,
)

object IndexCache {
    private val gson = Gson()

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
