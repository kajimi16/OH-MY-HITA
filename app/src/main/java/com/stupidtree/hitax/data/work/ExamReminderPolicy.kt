package com.stupidtree.hitax.data.work

import android.content.Context
import com.stupidtree.hitax.market.PresetStateStore

/**
 * 考试提醒是 HITA_Android 3.3.x 的行为。
 *
 * 3.1.2、HITA-L、CUTE 即使开着课程提醒，也不能收到考试通知。
 */
object ExamReminderPolicy {
    private const val ANDROID_33_PREFIX = "hita-android-3.3."

    fun isEnabled(context: Context): Boolean =
        isEnabledForPreset(PresetStateStore.activeId(context))

    fun isEnabledForPreset(presetId: String?): Boolean =
        presetId?.startsWith(ANDROID_33_PREFIX) == true
}
