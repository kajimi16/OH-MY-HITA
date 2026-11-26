package com.stupidtree.hitax.campus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CampusCatalogTest {

    @Test
    fun idsAndDisplayOrderAreStable() {
        assertEquals(
            listOf("hit.shenzhen", "hit.harbin", "hit.weihai"),
            CampusCatalog.all.map { it.id },
        )
        assertEquals(listOf("深圳", "本部", "威海"), CampusCatalog.all.map { it.displayName })
        assertEquals(CampusCatalog.all.size, CampusCatalog.all.map { it.id }.toSet().size)
    }

    @Test
    fun defaultCampusSupportsApiLogin() {
        assertEquals(CampusCatalog.SHENZHEN, CampusCatalog.default.id)
        assertTrue(CampusCatalog.default.supportsApiLogin)
        assertTrue(CampusCatalog.default.supportsWebLogin)
        assertNull(CampusCatalog.default.unavailableNote)
    }

    @Test
    fun onlyShenzhenIsApiCapable() {
        assertEquals(listOf(CampusCatalog.SHENZHEN), CampusCatalog.apiLoginProviders().map { it.id })
        CampusCatalog.all.filter { it.id != CampusCatalog.SHENZHEN }.forEach { provider ->
            assertFalse(provider.id, provider.supportsApiLogin)
            assertTrue(provider.id, provider.supportsWebLogin)
            assertEquals(CampusCatalog.API_UNAVAILABLE_NOTE, provider.unavailableNote)
        }
    }

    @Test
    fun lookupByIdToleratesUnknownValues() {
        assertEquals(CampusCatalog.HARBIN, CampusCatalog.byId("hit.harbin")?.id)
        assertNull(CampusCatalog.byId("hit.unknown"))
        assertNull(CampusCatalog.byId(""))
        assertNull(CampusCatalog.byId(null))
        CampusCatalog.all.forEach { provider ->
            assertNotNull(CampusCatalog.byId(provider.id))
        }
    }
}
