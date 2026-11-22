package com.stupidtree.hitax.market

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PresetShellMappingTest {

    @Test
    fun litePresetFallsBackToLiteShell() {
        assertEquals(
            PresetShellRouter.HITA_LITE_SHELL,
            PresetStateStore.shellForPreset("hita-lite-2.0", null),
        )
    }

    @Test
    fun declaredShellWinsForLiteAndOtherPresets() {
        assertEquals(
            "custom-shell",
            PresetStateStore.shellForPreset("hita-lite-2.0", "custom-shell"),
        )
        assertEquals(
            "declared",
            PresetStateStore.shellForPreset("hita-l-1.5.3", "declared"),
        )
        assertNull(PresetStateStore.shellForPreset("hita-l-1.5.3", null))
    }
}
