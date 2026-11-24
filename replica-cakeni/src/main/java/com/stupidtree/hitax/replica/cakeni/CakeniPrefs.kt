package com.stupidtree.hitax.replica.cakeni

import android.content.Context

/** 仅保存复刻 Shell 的展示偏好；不碰课表或插件数据。 */
object CakeniPrefs {
    private const val SP = "cakeni_replica"
    private const val DARK = "dark"
    private const val WEEKEND = "show_weekend"
    private const val NON_CURRENT = "show_non_current"
    private const val PERIOD_TIME = "show_period_time"

    private fun prefs(context: Context) = context.getSharedPreferences(SP, Context.MODE_PRIVATE)

    fun dark(context: Context): Boolean? {
        val p = prefs(context)
        return if (p.contains(DARK)) p.getBoolean(DARK, false) else null
    }

    fun setDark(context: Context, value: Boolean?) {
        prefs(context).edit().apply {
            if (value == null) remove(DARK) else putBoolean(DARK, value)
        }.apply()
    }

    fun showWeekend(context: Context): Boolean = prefs(context).getBoolean(WEEKEND, false)
    fun setShowWeekend(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(WEEKEND, value).apply()

    fun showNonCurrent(context: Context): Boolean = prefs(context).getBoolean(NON_CURRENT, true)
    fun setShowNonCurrent(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(NON_CURRENT, value).apply()

    fun showPeriodTime(context: Context): Boolean = prefs(context).getBoolean(PERIOD_TIME, true)
    fun setShowPeriodTime(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(PERIOD_TIME, value).apply()
}
