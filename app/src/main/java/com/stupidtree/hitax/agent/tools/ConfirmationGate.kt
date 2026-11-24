package com.stupidtree.hitax.agent.tools

/**
 * 写操作确认请求。UI 层只需展示标题、明细与两个按钮文案。
 */
data class ConfirmationRequest(
    val title: String,
    val detailLines: List<String> = emptyList(),
    val confirmLabel: String = "执行",
    val cancelLabel: String = "取消",
)

/**
 * 写操作确认通道。
 *
 * 默认 [unavailable]：没有 UI 订阅时写操作不得执行，避免后台/无界面路径静默落盘。
 */
fun interface ConfirmationGate {

    suspend fun request(request: ConfirmationRequest): Decision

    enum class Decision { APPROVED, REJECTED, UNAVAILABLE }

    companion object {
        val unavailable: ConfirmationGate = ConfirmationGate { Decision.UNAVAILABLE }
    }
}
