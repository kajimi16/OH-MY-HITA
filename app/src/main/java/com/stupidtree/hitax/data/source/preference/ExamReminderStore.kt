package com.stupidtree.hitax.data.source.preference

import android.content.Context
import android.content.SharedPreferences
import com.stupidtree.hitax.utils.TimeTools
import java.util.Calendar

/**
 * 考试提醒的持久化状态。
 *
 * 已发送的档位按自然日保存：跨天自动清空，避免长期累积。
 * 开关不单独存储，复用课程提醒总开关——课表类通知统一由它控制。
 */
class ExamReminderStore(private val context: Context) {

    private val preference: SharedPreferences
        get() = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    /** 跟随课程提醒总开关；关闭课程提醒即关闭所有课表相关通知。仅 3.3.x 生效。 */
    fun isEnabled(): Boolean =
        com.stupidtree.hitax.data.work.ExamReminderPolicy.isEnabled(context) &&
            CourseReminderStore.getInstance(context).isEnabled()

    /** 今天已发送的提醒 key；日期变化时返回空集，不继承昨天的记录。 */
    fun getSentKeys(): Set<String> {
        if (preference.getString(KEY_DATE, "") != today()) return emptySet()
        return preference.getStringSet(KEY_SENT_IDS, emptySet()).orEmpty().toSet()
    }

    /** 追加已发送的 key，并刷新日期。 */
    fun markSent(keys: Collection<String>) {
        if (keys.isEmpty()) return
        val merged = getSentKeys() + keys
        preference.edit()
            .putStringSet(KEY_SENT_IDS, HashSet(merged))
            .putString(KEY_DATE, today())
            .apply()
    }

    fun clearSent() {
        preference.edit().clear().apply()
    }

    private fun today(): String = TimeTools.getDateString(Calendar.getInstance())

    companion object {
        private const val SP_NAME = "exam_reminder_sent"
        private const val KEY_SENT_IDS = "sent_ids"
        private const val KEY_DATE = "date"

        @Volatile
        private var instance: ExamReminderStore? = null

        fun getInstance(context: Context): ExamReminderStore =
            instance ?: synchronized(this) {
                instance ?: ExamReminderStore(context.applicationContext).also { instance = it }
            }
    }
}
