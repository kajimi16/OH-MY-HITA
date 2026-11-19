package com.stupidtree.hitax.reminder

import com.stupidtree.hita.api.ReminderNotification
import com.stupidtree.hita.api.ReminderSpec

/** 纯规则：便于 JVM 测试，不依赖 Android。 */
internal object ReminderRules {

    const val MAX_TITLE = 120
    const val MAX_TEXT = 500

    /** hook 改写后的通知在进入 NotificationCompat 之前收敛，避免空白/超长/越界优先级。 */
    fun normalizeNotification(
        payload: ReminderNotification,
        fallback: ReminderNotification,
    ): ReminderNotification = ReminderNotification(
        title = payload.title.trim().take(MAX_TITLE).ifBlank { fallback.title.take(MAX_TITLE) },
        text = payload.text.trim().take(MAX_TEXT).ifBlank { fallback.text.take(MAX_TEXT) },
        priority = when {
            payload.priority == Int.MIN_VALUE -> Int.MIN_VALUE
            payload.priority < -2 -> -2
            payload.priority > 2 -> 2
            else -> payload.priority
        },
    )

    fun validate(spec: ReminderSpec, nowMillis: Long): String? = when {
        spec.ownerId.isBlank() -> "ownerId 不能为空"
        spec.id.isBlank() -> "id 不能为空"
        spec.triggerAtMillis <= nowMillis -> "提醒时间必须晚于当前时间"
        spec.title.isBlank() -> "标题不能为空"
        spec.title.length > MAX_TITLE -> "标题过长"
        spec.text.length > MAX_TEXT -> "正文过长"
        else -> null
    }
}
