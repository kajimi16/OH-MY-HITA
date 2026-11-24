package com.stupidtree.hitax.agent.tools

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WriteConfirmationTest {

    private val request = ConfirmationRequest(
        title = "确认添加活动",
        detailLines = listOf("活动：复习"),
    )

    @Test
    fun approvedGateProceeds() = runBlocking {
        val decision = WriteConfirmation.decide(
            gate = ConfirmationGate { ConfirmationGate.Decision.APPROVED },
            request = request,
            pendingText = "pending",
            cancelledText = "cancelled",
        )
        assertEquals(WriteConfirmation.Decision.Proceed, decision)
    }

    @Test
    fun rejectedGateDoesNotExecute() = runBlocking {
        val decision = WriteConfirmation.decide(
            gate = ConfirmationGate { ConfirmationGate.Decision.REJECTED },
            request = request,
            pendingText = "pending",
            cancelledText = "cancelled",
        )
        assertEquals("cancelled", (decision as WriteConfirmation.Decision.Halt).text)
    }

    @Test
    fun unavailableGateFailsClosed() = runBlocking {
        val decision = WriteConfirmation.decide(
            gate = ConfirmationGate.unavailable,
            request = request,
            pendingText = "pending",
            cancelledText = "cancelled",
        )
        assertTrue(decision is WriteConfirmation.Decision.Halt)
        assertEquals("pending", (decision as WriteConfirmation.Decision.Halt).text)
    }
}
