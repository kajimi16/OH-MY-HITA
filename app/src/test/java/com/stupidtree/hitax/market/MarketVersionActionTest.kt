package com.stupidtree.hitax.market

import org.junit.Assert.assertEquals
import org.junit.Test

class MarketVersionActionTest {

    @Test
    fun selectedOlderVersionOffersInstallEvenWhenInstalled() {
        assertEquals(
            MarketVersionAction.Kind.INSTALL,
            MarketVersionAction.resolve(
                installed = true,
                selectedVersion = "0.0.9",
                latestVersion = "0.1.0",
                updateAvailable = false,
            ),
        )
    }

    @Test
    fun installedLatestWithoutUpdateHasNoPrimaryAction() {
        assertEquals(
            MarketVersionAction.Kind.NONE,
            MarketVersionAction.resolve(
                installed = true,
                selectedVersion = "0.1.0",
                latestVersion = "0.1.0",
                updateAvailable = false,
            ),
        )
    }

    @Test
    fun installedOlderThanLatestOffersUpdate() {
        assertEquals(
            MarketVersionAction.Kind.UPDATE,
            MarketVersionAction.resolve(
                installed = true,
                selectedVersion = "0.1.0",
                latestVersion = "0.1.0",
                updateAvailable = true,
            ),
        )
    }

    @Test
    fun notInstalledOffersInstall() {
        assertEquals(
            MarketVersionAction.Kind.INSTALL,
            MarketVersionAction.resolve(
                installed = false,
                selectedVersion = "0.1.0",
                latestVersion = "0.1.0",
                updateAvailable = false,
            ),
        )
    }
}
