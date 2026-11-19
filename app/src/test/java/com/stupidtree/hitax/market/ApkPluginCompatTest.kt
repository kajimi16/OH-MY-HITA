package com.stupidtree.hitax.market

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkPluginCompatTest {

    @Test
    fun sameApiIsSupported() {
        assertTrue(ApkPluginCompat.isSupported(2, 2))
    }

    @Test
    fun olderPluginApiIsSupported() {
        assertTrue(ApkPluginCompat.isSupported(1, 2))
    }

    @Test
    fun newerPluginApiIsRejected() {
        assertFalse(ApkPluginCompat.isSupported(3, 2))
    }
}
