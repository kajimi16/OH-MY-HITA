package com.stupidtree.hitax.livecourse

import android.content.Context

/**
 * 直播课全局开关，对齐 HITA_Android 3.1.2「更多」页的同名开关。
 * 默认开启，保持现有直播课行为不变；关闭后只保留条目数据，不再拉起前台守护。
 */
internal object LiveCourseSettings {
    private const val PREFS = "live_courses"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}
