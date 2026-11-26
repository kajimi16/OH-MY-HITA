package com.stupidtree.hitax.eas

import com.stupidtree.hita.api.dto.EasReimportResultDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EasSessionServiceImplTest {

    private class FakeGateway : EasSessionGateway {
        var loggedIn = true
        var undergraduate = true
        var enabled = true
        var lastAt = 0L
        var logoutCalls = 0
        var setLastCalls = 0
        var importCalls = 0
        var throwOnImport = false
        var pending: ((Boolean) -> Unit)? = null
        var lastImportUndergraduate: Boolean? = null

        override fun isLoggedIn(): Boolean = loggedIn
        override fun isUndergraduate(): Boolean = undergraduate
        override fun logout() { logoutCalls++ }
        override fun autoReimportEnabled(): Boolean = enabled
        override fun lastAutoReimportAt(): Long = lastAt
        override fun setLastAutoReimportAt(ts: Long) {
            setLastCalls++
            lastAt = ts
        }

        override fun startImportCurrentTimetable(
            isUndergraduate: Boolean,
            onResult: (Boolean) -> Unit,
        ) {
            importCalls++
            lastImportUndergraduate = isUndergraduate
            if (throwOnImport) error("boom")
            pending = onResult
        }

        fun complete(success: Boolean) {
            pending?.invoke(success)
        }
    }

    @Test
    fun loggedOutCompletesExactlyOnceWithoutImporting() {
        val gateway = FakeGateway().apply { loggedIn = false }
        val results = mutableListOf<EasReimportResultDto>()
        EasSessionServiceImpl(gateway).startAutoReimport { results += it }

        assertEquals(1, results.size)
        assertFalse(results.single().success)
        assertEquals("NOT_LOGGED_IN", results.single().errorCode)
        assertEquals(0, gateway.importCalls)
    }

    @Test
    fun importCallbackIsDeduplicated() {
        val gateway = FakeGateway()
        val results = mutableListOf<EasReimportResultDto>()
        EasSessionServiceImpl(gateway).startAutoReimport { results += it }

        gateway.complete(true)
        gateway.complete(false)
        gateway.complete(true)

        assertEquals(1, results.size)
        assertTrue(results.single().success)
        assertEquals(1, gateway.setLastCalls)
        assertTrue(gateway.lastImportUndergraduate == true)
    }

    @Test
    fun importFailureIsReportedOnce() {
        val gateway = FakeGateway()
        val results = mutableListOf<EasReimportResultDto>()
        EasSessionServiceImpl(gateway).startAutoReimport { results += it }

        gateway.complete(false)

        assertEquals(1, results.size)
        assertFalse(results.single().success)
        assertEquals("IMPORT_FAILED", results.single().errorCode)
        assertEquals(0, gateway.setLastCalls)
    }

    @Test
    fun importExceptionIsReportedOnce() {
        val gateway = FakeGateway().apply { throwOnImport = true }
        val results = mutableListOf<EasReimportResultDto>()
        EasSessionServiceImpl(gateway).startAutoReimport { results += it }

        assertEquals(1, results.size)
        assertFalse(results.single().success)
        assertEquals("IMPORT_ERROR", results.single().errorCode)
    }

    @Test
    fun autoReimportSkipsWhenDisabled() {
        val gateway = FakeGateway().apply { enabled = false }
        EasSessionServiceImpl(gateway).autoReimportIfDue()
        assertEquals(0, gateway.importCalls)
    }

    @Test
    fun autoReimportSkipsWhenRecentlyImported() {
        val gateway = FakeGateway().apply { lastAt = System.currentTimeMillis() }
        EasSessionServiceImpl(gateway).autoReimportIfDue()
        assertEquals(0, gateway.importCalls)
    }

    @Test
    fun autoReimportRunsWhenDue() {
        val gateway = FakeGateway().apply { lastAt = System.currentTimeMillis() - 13 * 60 * 60 * 1000L }
        EasSessionServiceImpl(gateway).autoReimportIfDue()

        assertEquals(1, gateway.importCalls)
        gateway.complete(true)
        assertEquals(1, gateway.setLastCalls)
    }

    @Test
    fun logoutDelegates() {
        val gateway = FakeGateway()
        EasSessionServiceImpl(gateway).logout()
        assertEquals(1, gateway.logoutCalls)
    }
}
