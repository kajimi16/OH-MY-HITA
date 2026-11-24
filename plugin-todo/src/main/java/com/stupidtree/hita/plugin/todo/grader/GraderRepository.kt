package com.stupidtree.hita.plugin.todo.grader

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stupidtree.hita.plugin.todo.model.ImportedTodo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

internal enum class GraderError { AUTHENTICATION, NETWORK, STORAGE, INVALID_RESPONSE }

internal data class GraderState(
    val accountId: String? = null,
    val accountName: String? = null,
    val tasks: List<ImportedTodo> = emptyList(),
    val ready: Boolean = false,
    val syncing: Boolean = false,
    val needsLogin: Boolean = false,
    val error: GraderError? = null,
)

internal class GraderRepository(
    private val store: GraderStore,
    private val publicationDispatcher: CoroutineDispatcher,
    private val fetchRemote: (String, String?, () -> Unit) -> GraderRemoteSnapshot,
) {
    constructor(application: Application) : this(
        GraderPreferenceSource(application).let { source ->
            GraderStore(source::read, source::write, source::clear)
        },
        Dispatchers.Main.immediate,
        GraderWebSource()::fetch,
    )

    private val mutex = Mutex()
    @Volatile private var generation = 0L
    private var pendingChange: SessionChange? = null
    private var current = GraderState()
    private val mutableState = MutableLiveData(current)
    val state: LiveData<GraderState> = mutableState

    suspend fun reload(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val snapshot = loadSnapshot()
                if (!current.syncing) publish(snapshot.toState())
                true
            } catch (_: GraderStorageException) {
                publish(current.copy(ready = true, error = GraderError.STORAGE))
                false
            }
        }
    }

    suspend fun connect(cookieHeader: String, expectedAccountId: String? = null): Boolean =
        synchronize(cookieHeader, expectedAccountId)

    suspend fun refresh(): Boolean = synchronize(null, null)

    suspend fun setCompleted(accountId: String, id: String, completed: Boolean): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (pendingChange != null || current.accountId != accountId) return@withLock false
            try {
                val snapshot = store.setCompleted(accountId, id, completed) ?: return@withLock false
                publish(snapshot.toState(syncing = current.syncing).copy(needsLogin = current.needsLogin))
                true
            } catch (_: GraderStorageException) {
                publish(current.copy(error = GraderError.STORAGE))
                false
            }
        }
    }

    suspend fun disconnect(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            generation++
            pendingChange = SessionChange.DISCONNECT
            var error: GraderError? = null
            try {
                loadSnapshot()
                true
            } catch (_: GraderStorageException) {
                error = GraderError.STORAGE
                false
            } finally {
                // Even a failed commit hides the account until the durable change can be retried.
                publish(GraderState(ready = true, needsLogin = true, error = error))
            }
        }
    }

    /** 卸载并清除数据；只清持久层与内存快照，不改动 UI 状态。 */
    fun purge(): Boolean = store.purge()

    private fun loadSnapshot(): GraderSnapshot {
        val snapshot = when (pendingChange) {
            SessionChange.DISCONNECT -> store.disconnect()
            SessionChange.REPLACE -> store.clearForReplacement()
            null -> return store.load()
        }
        pendingChange = null
        return snapshot
    }

    private enum class SessionChange { DISCONNECT, REPLACE }

    private data class Operation(val generation: Long, val cookie: String, val expectedAccountId: String?)
    private class Superseded : Exception()

    private suspend fun synchronize(candidateCookie: String?, candidateAccountId: String?): Boolean = withContext(Dispatchers.IO) {
        var startedGeneration: Long? = null
        try {
            val operation = mutex.withLock {
                // A refresh cannot supersede an explicit login while no account is shown.
                if (candidateCookie == null && current.syncing && current.accountId == null) return@withContext false
                val token = ++generation
                startedGeneration = token
                try {
                    val cookie: String
                    val expectedAccountId: String?
                    if (candidateCookie != null) {
                        publish(GraderState(ready = true, syncing = true))
                        coroutineContext.ensureActive()
                        // Retry a failed user disconnect before the replacement can retire any active session.
                        if (pendingChange == SessionChange.DISCONNECT) loadSnapshot()
                        pendingChange = SessionChange.REPLACE
                        loadSnapshot()
                        cookie = GraderSessionPolicy.sessionCookie(candidateCookie) ?: throw GraderAuthenticationException()
                        expectedAccountId = candidateAccountId
                    } else {
                        val snapshot = loadSnapshot()
                        if (snapshot.disconnected) {
                            publish(snapshot.toState())
                            return@withContext false
                        }
                        cookie = snapshot.cookie ?: throw GraderAuthenticationException()
                        expectedAccountId = snapshot.activeAccount?.id ?: throw GraderAuthenticationException()
                        publish(snapshot.toState(syncing = true))
                    }
                    Operation(token, cookie, expectedAccountId)
                } catch (_: GraderStorageException) {
                    publish(current.copy(ready = true, syncing = false, error = GraderError.STORAGE))
                    null
                } catch (_: GraderAuthenticationException) {
                    publish(current.copy(ready = true, syncing = false, needsLogin = true, error = GraderError.AUTHENTICATION))
                    null
                }
            } ?: return@withContext false

            try {
                val context = coroutineContext
                val remote = fetchRemote(operation.cookie, operation.expectedAccountId) {
                    context.ensureActive()
                    if (generation != operation.generation) throw Superseded()
                }
                context.ensureActive()
                mutex.withLock {
                    if (generation != operation.generation) return@withLock false
                    context.ensureActive()
                    val snapshot = store.replace(
                        remote.account,
                        operation.cookie,
                        remote.assignments,
                        remote.submittedAssignmentIds,
                    )
                    publish(snapshot.toState())
                    true
                }
            } catch (_: Superseded) {
                false
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                mutex.withLock {
                    if (generation != operation.generation) return@withLock false
                    val error = when (failure) {
                        is GraderAuthenticationException -> GraderError.AUTHENTICATION
                        is GraderNetworkException -> GraderError.NETWORK
                        is GraderStorageException -> GraderError.STORAGE
                        is GraderInvalidResponseException -> GraderError.INVALID_RESPONSE
                        else -> GraderError.INVALID_RESPONSE
                    }
                    publish(current.copy(syncing = false, needsLogin = current.needsLogin || error == GraderError.AUTHENTICATION, error = error))
                    false
                }
            }
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                mutex.withLock {
                    if (generation == startedGeneration) {
                        publish(current.copy(syncing = false, needsLogin = current.needsLogin || current.accountId == null))
                    }
                }
            }
            throw cancelled
        }
    }

    private fun GraderSnapshot.toState(syncing: Boolean = false): GraderState = GraderState(
        accountId = activeAccount?.id,
        accountName = activeAccount?.name,
        tasks = todoTasks(),
        ready = true,
        syncing = syncing,
        needsLogin = activeAccount == null && !syncing,
    )

    /** Returning callers can immediately inspect state.value; no asynchronous postValue race. */
    private suspend fun publish(next: GraderState) {
        current = next
        withContext(NonCancellable + publicationDispatcher) { mutableState.value = next }
    }
}
