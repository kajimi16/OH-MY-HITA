package com.stupidtree.hita.plugin.todo

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.stupidtree.hita.api.TodoAccountState
import com.stupidtree.hita.api.TodoError
import com.stupidtree.hita.api.TodoItem
import com.stupidtree.hita.api.TodoLoginState
import com.stupidtree.hita.api.TodoService
import com.stupidtree.hita.api.TodoSource
import com.stupidtree.hita.plugin.todo.geniusta.GeniusTaError
import com.stupidtree.hita.plugin.todo.geniusta.GeniusTaRepository
import com.stupidtree.hita.plugin.todo.geniusta.GeniusTaState
import com.stupidtree.hita.plugin.todo.grader.GraderError
import com.stupidtree.hita.plugin.todo.grader.GraderRepository
import com.stupidtree.hita.plugin.todo.grader.GraderState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * TodoService 实现：两个独立仓库各自持有加密会话与缓存，这里只做投影与合并。
 *
 * 线程约定：LiveData 的写入由仓库切到主线程；本类的读取发生在主线程观察者里。
 */
internal class TodoServiceImpl internal constructor(
    private val grader: GraderRepository,
    private val geniusTa: GeniusTaRepository,
    private val scope: CoroutineScope,
) : TodoService {

    constructor(application: Application) : this(
        GraderRepository(application),
        GeniusTaRepository(application),
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )

    @Volatile private var graderLoggedOut = false
    @Volatile private var geniusTaLoggedOut = false

    private val graderAccount = MediatorLiveData<TodoAccountState>().apply {
        postValue(TodoAccountState(TodoSource.GRADER))
        addSource(grader.state) { value = it.toPublic(graderLoggedOut) }
    }

    private val geniusTaAccount = MediatorLiveData<TodoAccountState>().apply {
        postValue(TodoAccountState(TodoSource.GENIUS_TA))
        addSource(geniusTa.state) { value = it.toPublic(geniusTaLoggedOut) }
    }

    private val mergedItems = MediatorLiveData<List<TodoItem>>().apply {
        postValue(emptyList())
        addSource(grader.state) { recomputeItems() }
        addSource(geniusTa.state) { recomputeItems() }
    }

    init {
        scope.launch {
            grader.reload()
            geniusTa.reload()
        }
    }

    override fun state(source: TodoSource): LiveData<TodoAccountState> = when (source) {
        TodoSource.GRADER -> graderAccount
        TodoSource.GENIUS_TA -> geniusTaAccount
    }

    override fun items(): LiveData<List<TodoItem>> = mergedItems

    override suspend fun connectGrader(cookieHeader: String, expectedAccountId: String?): Boolean {
        graderLoggedOut = false
        return grader.connect(cookieHeader, expectedAccountId)
    }

    override suspend fun fetchGraderAssignments(): Boolean = grader.refresh()

    override suspend fun loginGeniusTa(username: String, password: String): Boolean {
        geniusTaLoggedOut = false
        return geniusTa.connect(username, password)
    }

    override suspend fun importGeniusTaAssignments(): Boolean = geniusTa.refresh()

    override suspend fun setCompleted(source: TodoSource, id: String, completed: Boolean): Boolean =
        when (source) {
            TodoSource.GRADER -> grader.state.value?.accountId
                ?.let { grader.setCompleted(it, id, completed) } ?: false
            TodoSource.GENIUS_TA -> geniusTa.state.value?.accountId
                ?.let { geniusTa.setCompleted(it, id, completed) } ?: false
        }

    override suspend fun logout(source: TodoSource): Boolean = when (source) {
        TodoSource.GRADER -> {
            graderLoggedOut = true
            grader.disconnect()
        }
        TodoSource.GENIUS_TA -> {
            geniusTaLoggedOut = true
            geniusTa.disconnect()
        }
    }

    /** 卸载并清除数据；两个来源的加密快照都删除。 */
    fun purge() {
        grader.purge()
        geniusTa.purge()
    }

    fun close() {
        scope.cancel()
    }

    private fun recomputeItems() {
        val graderTasks = grader.state.value?.tasks.orEmpty()
        val geniusTasks = geniusTa.state.value?.tasks.orEmpty()
        val items = ArrayList<TodoItem>(graderTasks.size + geniusTasks.size)
        graderTasks.forEach {
            items += TodoItem(TodoSource.GRADER, it.id, it.title, it.courseTitle, it.dueAt, it.completed)
        }
        geniusTasks.forEach {
            items += TodoItem(TodoSource.GENIUS_TA, it.id, it.title, it.courseTitle, it.dueAt, it.completed)
        }
        items.sortWith(compareBy<TodoItem>({ it.dueAt }, { it.title }))
        mergedItems.value = items
    }

    private fun GraderState.toPublic(loggedOut: Boolean): TodoAccountState {
        val publicError = error.toPublic()
        return TodoAccountState(
            source = TodoSource.GRADER,
            loginState = loginState(loggedOut, accountId != null, needsLogin),
            accountId = accountId,
            accountName = accountName,
            syncing = syncing,
            itemCount = tasks.size,
            error = publicError,
        )
    }

    private fun GeniusTaState.toPublic(loggedOut: Boolean): TodoAccountState {
        val publicError = error.toPublic()
        return TodoAccountState(
            source = TodoSource.GENIUS_TA,
            loginState = loginState(loggedOut, accountId != null, needsLogin),
            accountId = accountId,
            accountName = null,
            syncing = syncing,
            itemCount = tasks.size,
            error = publicError,
        )
    }

    private fun loginState(loggedOut: Boolean, hasAccount: Boolean, needsLogin: Boolean): TodoLoginState = when {
        loggedOut -> TodoLoginState.LOGGED_OUT
        hasAccount && needsLogin -> TodoLoginState.EXPIRED
        hasAccount -> TodoLoginState.ACTIVE
        else -> TodoLoginState.LOGGED_OUT
    }

    private fun GraderError?.toPublic(): TodoError = when (this) {
        null -> TodoError.NONE
        GraderError.AUTHENTICATION -> TodoError.AUTHENTICATION
        GraderError.NETWORK -> TodoError.NETWORK
        GraderError.STORAGE -> TodoError.STORAGE
        GraderError.INVALID_RESPONSE -> TodoError.INVALID_RESPONSE
    }

    private fun GeniusTaError?.toPublic(): TodoError = when (this) {
        null -> TodoError.NONE
        GeniusTaError.AUTHENTICATION -> TodoError.AUTHENTICATION
        GeniusTaError.NETWORK -> TodoError.NETWORK
        GeniusTaError.STORAGE -> TodoError.STORAGE
        GeniusTaError.INVALID_RESPONSE -> TodoError.INVALID_RESPONSE
    }
}
