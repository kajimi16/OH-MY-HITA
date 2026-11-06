package com.stupidtree.hitax.market

import android.content.Context

/** 记录当前生效的整合包，供市场和首启选择显示“使用中”。 */
object PresetStateStore {
    private const val PREFS = "preset_state"
    private const val KEY_ACTIVE = "active_preset"

    fun activeId(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE, null)

    fun setActive(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ACTIVE, id).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_ACTIVE).apply()
    }
}
