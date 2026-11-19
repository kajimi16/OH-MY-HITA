package com.stupidtree.hitax.data

import com.stupidtree.hita.api.SyncRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncHookPolicyTest {

    @Test
    fun dataRewriteIsAccepted() {
        val original = SyncRecord("timetable", listOf("a"), listOf("old"))
        val expectedIds = listOf("a")
        val hooked = original.copy(data = listOf("merged"))

        assertEquals(
            hooked,
            SyncHookPolicy.accept(original, expectedIds, listOf("old"), hooked),
        )
    }

    @Test
    fun keyOrIdsRedirectFallsBackToOriginal() {
        val original = SyncRecord("timetable", listOf("a"), listOf("old"))
        val expectedIds = listOf("a")
        val redirected = original.copy(key = "subject", ids = listOf("a", "b"))

        assertEquals(
            original,
            SyncHookPolicy.accept(original, expectedIds, listOf("old"), redirected),
        )
    }

    @Test
    fun inPlaceIdsMutationFallsBackToOriginal() {
        val originalIds = mutableListOf("a")
        val original = SyncRecord("timetable", originalIds, listOf("old"))
        val expectedIds = listOf("a")
        originalIds += "b"

        val restored = SyncHookPolicy.accept(original, expectedIds, listOf("old"), original)
        assertEquals(listOf("a"), restored.ids)
        assertEquals(listOf("old"), restored.data)
    }
}
