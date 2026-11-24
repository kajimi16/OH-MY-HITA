package com.stupidtree.hitax.agent

import com.stupidtree.hitax.agent.tools.ConfirmationGate
import com.stupidtree.hitax.agent.tools.ConfirmationRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatConfirmationGateTest {

    private val request = ConfirmationRequest(
        title = "确认添加活动",
        detailLines = listOf("活动：复习"),
    )

    @Test
    fun approveResolvesPendingRequestAndClearsState() = runBlocking {
        val gate = ChatConfirmationGate()
        var decision: ConfirmationGate.Decision? = null
        val job = launch { decision = gate.request(request) }
        while (gate.state.value == null) yield()

        gate.approve()
        job.join()

        assertEquals(ConfirmationGate.Decision.APPROVED, decision)
        assertNull(gate.state.value)
    }

    @Test
    fun rejectResolvesPendingRequest() = runBlocking {
        val gate = ChatConfirmationGate()
        var decision: ConfirmationGate.Decision? = null
        val job = launch { decision = gate.request(request) }
        while (gate.state.value == null) yield()

        gate.reject()
        job.join()

        assertEquals(ConfirmationGate.Decision.REJECTED, decision)
    }

    @Test
    fun secondRequestWhilePendingIsUnavailable() = runBlocking {
        val gate = ChatConfirmationGate()
        val first = launch { gate.request(request) }
        while (gate.state.value == null) yield()

        assertEquals(
            ConfirmationGate.Decision.UNAVAILABLE,
            gate.request(request),
        )

        gate.reject()
        first.join()
    }
}
