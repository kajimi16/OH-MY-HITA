package com.stupidtree.hita.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PluginToolSpecTest {

    @Test
    fun legacyFiveArgumentConstructorKeepsDefaults() {
        val spec = PluginToolSpec("lookup", "desc", emptyList(), 2048, 9000)

        assertEquals(2048, spec.maxObservationChars)
        assertEquals(9000, spec.timeoutMs)
        assertEquals("", spec.outputSchemaJson)
        assertFalse(spec.retrySafe)
        assertFalse(spec.allowRawString)
    }

    @Test
    fun newFieldsAreOptional() {
        val spec = PluginToolSpec(
            name = "lookup",
            description = "desc",
            outputSchemaJson = """{"type":"string"}""",
            retrySafe = true,
            allowRawString = true,
        )

        assertEquals("""{"type":"string"}""", spec.outputSchemaJson)
        assertEquals(1800, spec.maxObservationChars)
        assertEquals(15_000, spec.timeoutMs)
    }
}
