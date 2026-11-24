package com.stupidtree.hitax.data.source.preference

import android.content.Context
import android.content.SharedPreferences

/**
 * 课程提醒设置存储
 */
class CourseReminderStore(private val context: Context) {
    private val preference: SharedPreferences
        get() = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    /**
     * 是否开启课程提醒
     */
    fun isEnabled(): Boolean {
        return preference.getBoolean(KEY_ENABLED, false)
    }

    /**
     * 设置课程提醒开关
     */
    fun setEnabled(enabled: Boolean) {
        preference.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * 获取提前提醒时间（分钟）
     */
    fun getReminderMinutes(): Int {
        return preference.getInt(KEY_MINUTES, DEFAULT_MINUTES)
    }

    /**
     * 设置提前提醒时间（分钟）
     */
    fun setReminderMinutes(minutes: Int) {
        preference.edit().putInt(KEY_MINUTES, minutes).apply()
    }

    /** 3.3.x 下课前提醒；旧版本默认关闭，保持原行为。 */
    fun isEndReminderEnabled(): Boolean =
        preference.getBoolean(KEY_END_ENABLED, false)

    fun setEndReminderEnabled(enabled: Boolean) {
        preference.edit().putBoolean(KEY_END_ENABLED, enabled).apply()
    }

    fun getEndReminderMinutes(): Int =
        preference.getInt(KEY_END_MINUTES, DEFAULT_END_MINUTES)

    fun setEndReminderMinutes(minutes: Int) {
        preference.edit().putInt(KEY_END_MINUTES, minutes.coerceIn(1, 60)).apply()
    }

    companion object {
        private const val SP_NAME = "course_reminder"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_MINUTES = "minutes"
        private const val KEY_END_ENABLED = "end_enabled"
        private const val KEY_END_MINUTES = "end_minutes"
        private const val DEFAULT_MINUTES = 10
        private const val DEFAULT_END_MINUTES = 5

        @Volatile
        private var instance: CourseReminderStore? = null

        fun getInstance(context: Context): CourseReminderStore {
            return instance ?: synchronized(this) {
                instance ?: CourseReminderStore(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
