package com.stupidtree.hitax.reminder

import com.stupidtree.hita.api.ReminderNotification
import com.stupidtree.hita.api.ReminderSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderRulesTest {

    private fun spec(
        ownerId: String = "plugin.test",
        id: String = "r1",
        at: Long = 1_000L,
        title: String = "上课",
        text: String = "高等数学",
    ) = ReminderSpec(ownerId, id, at, title, text)

    @Test fun validReminderAccepted() {
        assertNull(ReminderRules.validate(spec(at = 2_000), nowMillis = 1_000))
    }

    @Test fun pastReminderRejected() {
        assertEquals("提醒时间必须晚于当前时间", ReminderRules.validate(spec(at = 999), 1_000))
        assertEquals("提醒时间必须晚于当前时间", ReminderRules.validate(spec(at = 1_000), 1_000))
    }

    @Test fun blankFieldsRejected() {
        assertEquals("ownerId 不能为空", ReminderRules.validate(spec(ownerId = " "), 0))
        assertEquals("id 不能为空", ReminderRules.validate(spec(id = ""), 0))
        assertEquals("标题不能为空", ReminderRules.validate(spec(title = ""), 0))
    }

    @Test fun notificationRewriteIsClampedAndFallsBackWhenBlank() {
        val fallback = ReminderNotification("原题", "原文")
        val rewritten = ReminderRules.normalizeNotification(
            ReminderNotification("x".repeat(200), " ", 99),
            fallback,
        )

        assertEquals(ReminderRules.MAX_TITLE, rewritten.title.length)
        assertEquals("原文", rewritten.text)
        assertEquals(2, rewritten.priority)
    }

    @Test fun notificationSentinelPriorityIsPreserved() {
        val fallback = ReminderNotification("原题", "原文")
        val rewritten = ReminderRules.normalizeNotification(
            ReminderNotification("题", "文"),
            fallback,
        )

        assertEquals(Int.MIN_VALUE, rewritten.priority)
    }

    @Test fun lengthLimitsEnforced() {
        assertEquals("标题过长", ReminderRules.validate(spec(title = "x".repeat(121)), 0))
        assertEquals("正文过长", ReminderRules.validate(spec(text = "x".repeat(501)), 0))
        assertNull(ReminderRules.validate(spec(title = "x".repeat(120), text = "x".repeat(500)), 0))
    }
}
