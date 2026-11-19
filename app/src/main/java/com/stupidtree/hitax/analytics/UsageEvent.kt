package com.stupidtree.hitax.analytics

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_event")
data class UsageEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val installId: String,
    val event: String,
    val screen: String? = null,
    val pluginId: String? = null,
    val appVersion: String,
    val sdk: Int,
    val timestamp: Long,
)

/** 只允许这些事件进入队列，避免误传自由文本。 */
object UsageEventAllowlist {
    const val APP_OPEN = "app_open"
    const val SCREEN_VIEW = "screen_view"
    const val PLUGIN_OPEN = "plugin_open"
    const val PLUGIN_INSTALL = "plugin_install"
    const val PLUGIN_MARKET_VIEW = "plugin_market_view"
    const val TIMETABLE_OPEN = "timetable_open"
    const val TODAY_OPEN = "today_open"
    const val MORE_OPEN = "more_open"

    val all = setOf(
        APP_OPEN, SCREEN_VIEW, PLUGIN_OPEN, PLUGIN_INSTALL,
        PLUGIN_MARKET_VIEW, TIMETABLE_OPEN, TODAY_OPEN, MORE_OPEN,
    )
}
