package com.stupidtree.hitax.agent

import com.stupidtree.hitax.agent.tools.ConfirmationGate
import com.stupidtree.hitax.agent.tools.ConfirmationRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 聊天页的写操作确认门。
 *
 * 同一时刻只允许一个待确认请求；重复请求按 UNAVAILABLE 处理，写工具会 fail-closed。
 */
class ChatConfirmationGate : ConfirmationGate {

    private val lock = Any()
    private var outstanding: CompletableDeferred<ConfirmationGate.Decision>? = null
    private val _state = MutableStateFlow<ConfirmationRequest?>(null)

    /** 非空时聊天页展示确认对话框。 */
    val state: StateFlow<ConfirmationRequest?> = _state.asStateFlow()

    override suspend fun request(request: ConfirmationRequest): ConfirmationGate.Decision {
        val deferred = CompletableDeferred<ConfirmationGate.Decision>()
        synchronized(lock) {
            if (outstanding != null) return ConfirmationGate.Decision.UNAVAILABLE
            outstanding = deferred
            _state.value = request
        }
        return try {
            deferred.await()
        } finally {
            synchronized(lock) {
                if (outstanding === deferred) outstanding = null
                _state.value = null
            }
        }
    }

    fun approve() = complete(ConfirmationGate.Decision.APPROVED)

    fun reject() = complete(ConfirmationGate.Decision.REJECTED)

    fun cancelAll() = complete(ConfirmationGate.Decision.REJECTED)

    private fun complete(decision: ConfirmationGate.Decision) {
        val target = synchronized(lock) { outstanding }
        target?.complete(decision)
    }
}
