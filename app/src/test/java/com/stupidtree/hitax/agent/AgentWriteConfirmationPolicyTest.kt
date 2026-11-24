package com.stupidtree.hitax.agent

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentWriteConfirmationPolicyTest {

    @Test
    fun android331EnablesWriteConfirmation() {
        assertTrue(AgentWriteConfirmationPolicy.isEnabledForPreset("hita-android-3.3.1"))
        assertTrue(AgentWriteConfirmationPolicy.isEnabledForPreset("hita-android-3.3.2"))
    }

    @Test
    fun android312AndLegacyPresetsKeepOldBehavior() {
        assertFalse(AgentWriteConfirmationPolicy.isEnabledForPreset("hita-android-3.1.2"))
        assertFalse(AgentWriteConfirmationPolicy.isEnabledForPreset("hita-l-1.5.3"))
        assertFalse(AgentWriteConfirmationPolicy.isEnabledForPreset(null))
    }
}
