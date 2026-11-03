package com.stupidtree.hitax.utils

/**
 * 更新提示的节流策略。
 *
 * - 用户点「跳过此版本」→ 该版本永久不再提示；
 * - 用户点「取消」→ 一天内不再提示，之后恢复。
 */
object UpdatePromptPolicy {

    const val CANCEL_COOLDOWN_MS: Long = 24L * 60 * 60 * 1000

    fun shouldPrompt(skipped: Boolean, canceledAt: Long, now: Long): Boolean {
        if (skipped) return false
        if (canceledAt <= 0L) return true
        return now - canceledAt >= CANCEL_COOLDOWN_MS
    }
}
