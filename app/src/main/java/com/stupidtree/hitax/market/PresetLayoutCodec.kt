package com.stupidtree.hitax.market

import com.google.gson.Gson

/**
 * 预设底栏布局的持久化编码。
 *
 * 单独抽成纯对象，保证 active preset、shell 与 tabs 可以写入同一个
 * SharedPreferences 文件，也便于在 JVM 上验证损坏数据的回退行为。
 */
internal object PresetLayoutCodec {
    private val gson = Gson()

    fun encode(tabs: List<PresetTab>?): String? {
        val valid = tabs.orEmpty().filter { it.id.isNotBlank() }
        return valid.takeIf { it.isNotEmpty() }?.let { gson.toJson(it) }
    }

    fun decode(json: String?): List<PresetTab> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson(json, Array<PresetTab>::class.java)?.toList().orEmpty()
        }.getOrDefault(emptyList()).filter { it.id.isNotBlank() }
    }
}
