package com.stupidtree.hitax.data.work

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamReminderPolicyTest {

    @Test
    fun onlyAndroid33PresetsEnableExamReminders() {
        assertTrue(ExamReminderPolicy.isEnabledForPreset("hita-android-3.3.1"))
        assertTrue(ExamReminderPolicy.isEnabledForPreset("hita-android-3.3.2"))
    }

    @Test
    fun legacyPresetsNeverEnableExamReminders() {
        assertFalse(ExamReminderPolicy.isEnabledForPreset("hita-android-3.1.2"))
        assertFalse(ExamReminderPolicy.isEnabledForPreset("hita-l-1.5.3"))
        assertFalse(ExamReminderPolicy.isEnabledForPreset("hita-cute-1.6.0"))
        assertFalse(ExamReminderPolicy.isEnabledForPreset(null))
    }
}
