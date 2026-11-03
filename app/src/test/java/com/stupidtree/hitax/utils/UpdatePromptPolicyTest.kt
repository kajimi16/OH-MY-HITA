package com.stupidtree.hitax.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePromptPolicyTest {

    private val now = 1_800_000_000_000L
    private val day = UpdatePromptPolicy.CANCEL_COOLDOWN_MS

    @Test
    fun promptsWhenNeverCanceled() {
        assertTrue(UpdatePromptPolicy.shouldPrompt(skipped = false, canceledAt = 0L, now = now))
    }

    @Test
    fun silentWithinOneDayAfterCancel() {
        assertFalse(UpdatePromptPolicy.shouldPrompt(false, now - 60_000, now))
        assertFalse(UpdatePromptPolicy.shouldPrompt(false, now - day + 1_000, now))
    }

    @Test
    fun promptsAgainAfterOneDay() {
        assertTrue(UpdatePromptPolicy.shouldPrompt(false, now - day, now))
        assertTrue(UpdatePromptPolicy.shouldPrompt(false, now - 2 * day, now))
    }

    @Test
    fun skippedVersionNeverPrompts() {
        assertFalse(UpdatePromptPolicy.shouldPrompt(true, 0L, now))
        assertFalse(UpdatePromptPolicy.shouldPrompt(true, now - 10 * day, now))
    }
}
