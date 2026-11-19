package com.stupidtree.hitax.data.source.preference

import android.content.Context

/** 「更多 → 使用统计」开关。当前版本只持久化用户偏好，不上传事件。 */
object UsageStatsStore {
    private const val PREFS = "usage_stats"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_ASKED = "asked"

    fun isEnabled(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).putBoolean(KEY_ASKED, true).apply()
    }

    /** 是否已经完成首次统计询问。 */
    fun hasAsked(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ASKED, false)

    fun markAsked(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ASKED, true).apply()
    }
}
