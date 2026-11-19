package com.stupidtree.hitax.market

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginTrustTest {

    @Test
    fun defaultIsUnverified() {
        assertEquals(PluginTrust.Level.UNVERIFIED, PluginTrust.level(emptySet(), "any.plugin"))
    }

    @Test
    fun allowlistMarksVerified() {
        val verified = PluginTrust.parseVerifiedIds(
            """{"verified":[{"id":"com.example.hello"},{"id":"com.example.sample"}]}"""
        )
        assertEquals(PluginTrust.Level.VERIFIED, PluginTrust.level(verified, "com.example.hello"))
        assertEquals(PluginTrust.Level.UNVERIFIED, PluginTrust.level(verified, "com.other"))
    }

    @Test
    fun malformedJsonMeansNothingVerified() {
        assertTrue(PluginTrust.parseVerifiedIds("not json").isEmpty())
        assertTrue(PluginTrust.parseVerifiedIds("{}").isEmpty())
    }
}
