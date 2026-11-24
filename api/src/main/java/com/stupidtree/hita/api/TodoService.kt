package com.stupidtree.hita.api

import androidx.lifecycle.LiveData

/** 待办来源。Grader 作业与 GeniusTA 作业分开登录、分开缓存。 */
enum class TodoSource { GRADER, GENIUS_TA }

/** 单个来源的登录态。到期待办仍可读缓存，但刷新会失败。 */
enum class TodoLoginState { LOGGED_OUT, ACTIVE, EXPIRED }

/** 最近一次操作的失败原因；[NONE] 表示当前没有错误。 */
enum class TodoError { NONE, AUTHENTICATION, NETWORK, STORAGE, INVALID_RESPONSE }

/** 单个来源的账号状态快照，只含展示信息，不含 cookie 与密码。 */
data class TodoAccountState(
    val source: TodoSource,
    val loginState: TodoLoginState = TodoLoginState.LOGGED_OUT,
    val accountId: String? = null,
    val accountName: String? = null,
    val syncing: Boolean = false,
    val itemCount: Int = 0,
    val error: TodoError = TodoError.NONE,
)

/** 合并列表里的一条待办。完成状态是本地记录，不回写远端。 */
data class TodoItem(
    val source: TodoSource,
    val id: String,
    val title: String,
    val courseTitle: String,
    val dueAt: Long,
    val completed: Boolean,
)

/**
 * 待办聚合服务（API 11）。由 plugin-todo 提供。
 *
 * 所有写操作都是显式 suspend 方法，返回是否成功；界面通过 LiveData 观察结果。
 * Grader 登录依赖浏览器/CAS 会话，本契约只接收会话 cookie，不包含 WebView 流程。
 * GeniusTA 使用账号密码登录，凭据只存在插件私有加密存储中。
 */
interface TodoService {

    /** 单个来源的账号状态；首次订阅时立即推送当前缓存。 */
    fun state(source: TodoSource): LiveData<TodoAccountState>

    /** 两个来源合并后的未删除待办，按截止时间升序。 */
    fun items(): LiveData<List<TodoItem>>

    /**
     * 用已验证的 Grader 浏览器会话连接账号。
     *
     * [cookieHeader] 必须是包含 `sessionid` 的 Cookie 头；[expectedAccountId]
     * 非空时要求会话账号与之一致。会话无效、账号不符或网络失败返回 false。
     */
    suspend fun connectGrader(cookieHeader: String, expectedAccountId: String? = null): Boolean

    /** 用已保存的会话重新拉取 Grader 课程与作业。未登录或会话过期返回 false。 */
    suspend fun fetchGraderAssignments(): Boolean

    /** 用账号密码登录 GeniusTA，成功后立即导入未提交作业。凭据不进入 UI。 */
    suspend fun loginGeniusTa(username: String, password: String): Boolean

    /** 用已保存的 GeniusTA 会话重新导入作业。未登录或会话过期返回 false。 */
    suspend fun importGeniusTaAssignments(): Boolean

    /** 切换本地完成状态。来源未登录、条目不存在或条目已被远端提交时返回 false。 */
    suspend fun setCompleted(source: TodoSource, id: String, completed: Boolean): Boolean

    /** 退出单个来源；保留该账号的历史缓存，但不再展示。 */
    suspend fun logout(source: TodoSource): Boolean

    companion object {
        val KEY = ServiceKey<TodoService>("hita.todo")
    }
}
