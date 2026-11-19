package com.stupidtree.hitax.market

/**
 * 插件版本比较。
 *
 * 支持 `1.2.3`、`v1.2`、带预发布后缀的 `1.0.0-beta.2`：
 * 数字段按数值比较，缺失段视为 0；同一主版本下预发布小于正式版。
 */
object PluginVersion {

    fun compare(a: String, b: String): Int {
        val (an, apre) = split(a)
        val (bn, bpre) = split(b)
        val size = maxOf(an.size, bn.size)
        for (i in 0 until size) {
            val x = an.getOrElse(i) { 0 }
            val y = bn.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return when {
            apre == null && bpre == null -> 0
            apre == null -> 1
            bpre == null -> -1
            else -> comparePreRelease(apre, bpre)
        }
    }

    /** 是否有可更新版本；相同或更旧都不提示。 */
    fun isNewer(available: String, installed: String): Boolean =
        compare(available, installed) > 0

    private fun split(raw: String): Pair<List<Int>, String?> {
        val text = raw.trim().removePrefix("v").removePrefix("V")
        if (text.isEmpty()) return emptyList<Int>() to null
        val preIndex = text.indexOfFirst { it == '-' || it == '+' }
        val numbersPart = if (preIndex >= 0) text.substring(0, preIndex) else text
        val pre = if (preIndex >= 0) text.substring(preIndex + 1).ifBlank { null } else null
        val numbers = numbersPart.split('.').map { it.toIntOrNull() ?: 0 }
        return numbers to pre
    }

    private fun comparePreRelease(a: String, b: String): Int {
        val as1 = a.split('.')
        val bs1 = b.split('.')
        for (i in 0 until maxOf(as1.size, bs1.size)) {
            val x = as1.getOrNull(i) ?: return -1
            val y = bs1.getOrNull(i) ?: return 1
            val nx = x.toIntOrNull()
            val ny = y.toIntOrNull()
            val cmp = if (nx != null && ny != null) nx.compareTo(ny) else x.compareTo(y)
            if (cmp != 0) return cmp
        }
        return 0
    }
}
