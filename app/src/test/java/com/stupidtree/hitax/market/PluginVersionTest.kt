package com.stupidtree.hitax.market

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginVersionTest {

    @Test
    fun numericSegmentsCompareAsNumbers() {
        assertTrue(PluginVersion.isNewer("0.4.10", "0.4.9"))
        assertTrue(PluginVersion.isNewer("0.10.0", "0.9.9"))
        assertFalse(PluginVersion.isNewer("0.9.9", "0.10.0"))
    }

    @Test
    fun missingSegmentsAreZero() {
        assertEquals(0, PluginVersion.compare("1.0", "1.0.0"))
        assertEquals(0, PluginVersion.compare("v1.2.3", "1.2.3"))
    }

    @Test
    fun prereleaseIsOlderThanRelease() {
        assertTrue(PluginVersion.isNewer("1.0.0", "1.0.0-beta"))
        assertTrue(PluginVersion.isNewer("1.0.0-beta.2", "1.0.0-beta.1"))
    }

    @Test
    fun sameVersionIsNotAnUpdate() {
        assertFalse(PluginVersion.isNewer("0.4.8", "0.4.8"))
    }
}
