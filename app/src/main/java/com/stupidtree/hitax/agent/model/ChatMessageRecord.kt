package com.stupidtree.hitax.agent.model

import com.stupidtree.hitax.agent.model.AgentResourceCard

/** Persisted chat content; presentation state such as placeholders belongs to the UI. */
data class ChatMessageRecord(
    val role: Role,
    val text: String,
    val resourceCards: List<AgentResourceCard> = emptyList(),
    val timestampMs: Long = System.currentTimeMillis(),
) {
    enum class Role { USER, ASSISTANT, TRACE }
}
