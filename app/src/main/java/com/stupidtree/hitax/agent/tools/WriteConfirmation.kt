package com.stupidtree.hitax.agent.tools

/**
 * 写操作确认的唯一决策点。
 *
 * 无 UI 订阅一律 Halt，不能把 UNAVAILABLE 当成“用户默认同意”。
 */
object WriteConfirmation {

    sealed interface Decision {
        data object Proceed : Decision
        data class Halt(val text: String) : Decision
    }

    suspend fun decide(
        gate: ConfirmationGate,
        request: ConfirmationRequest,
        pendingText: String,
        cancelledText: String,
    ): Decision = when (gate.request(request)) {
        ConfirmationGate.Decision.APPROVED -> Decision.Proceed
        ConfirmationGate.Decision.REJECTED -> Decision.Halt(cancelledText)
        ConfirmationGate.Decision.UNAVAILABLE -> Decision.Halt(pendingText)
    }
}
