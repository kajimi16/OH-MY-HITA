package com.stupidtree.hita.api

import org.junit.Assert.assertEquals
import org.junit.Test

class EasContractVersionTest {

    @Test
    fun easServiceKeysAreStable() {
        assertEquals("hita.eas.account", EasAccountService.KEY.name)
        assertEquals("hita.eas.session", EasSessionService.KEY.name)
        assertEquals("hita.eas.ui", EasUiService.KEY.name)
        assertEquals("hita.eas", EasService.KEY.name)
    }

    @Test
    fun apiVersionIncludesAdditiveEasContracts() {
        assertEquals(14, HITA_API_VERSION)
    }
}
