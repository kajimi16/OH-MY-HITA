package com.stupidtree.hitax.agent.data

import android.content.Context
import com.stupidtree.hitax.agent.model.ChatMessageRecord
import com.stupidtree.hitax.agent.model.ChatSession
import java.io.Closeable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class ChatSessionRepository private constructor(private val store: ChatSessionStore) : Closeable {
    private fun interface Operation { fun run() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val operations = Channel<Operation>(Channel.UNLIMITED)
    private val worker = scope.launch {
        for (operation in operations) operation.run()
    }.also { job -> job.invokeOnCompletion { scope.cancel() } }

    val sessions get() = store.sessions
    fun create(session: ChatSession): Deferred<Unit> = submit { store.create(session) }
    fun latest(): Deferred<ChatSession?> = submit { store.latest() }
    fun restore(sessionId: String): Deferred<List<ChatMessageRecord>> = submit { store.restore(sessionId) }
    fun delete(session: ChatSession): Deferred<ChatSession?> = submit { store.delete(session) }
    fun append(sessionId: String, message: ChatMessageRecord): Deferred<Boolean> =
        submit { store.append(sessionId, message.copy(resourceCards = message.resourceCards.toList())) }

    private fun <T> submit(action: () -> T): Deferred<T> {
        val completion = CompletableDeferred<T>()
        val operation = Operation {
            try { completion.complete(action()) }
            catch (error: Throwable) { completion.completeExceptionally(error) }
        }
        if (operations.trySend(operation).isFailure) {
            completion.completeExceptionally(IllegalStateException("Chat store is closed"))
        }
        return completion
    }

    override fun close() { operations.close() }

    companion object {
        @Volatile private var instance: ChatSessionRepository? = null
        fun get(context: Context): ChatSessionRepository = instance ?: synchronized(this) {
            instance ?: ChatSessionRepository(
                ChatSessionStore(ChatDatabase.get(context))
            ).also { instance = it }
        }
    }
}
