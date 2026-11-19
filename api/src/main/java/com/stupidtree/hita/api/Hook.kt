package com.stupidtree.hita.api

/**
 * 同步决策拦截：监听器拿到当前值，可调用 [next] 委托下游，也可直接返回短路。
 * 注册顺序就是执行顺序。监听器不得做阻塞 IO；宿主不提供超时。
 */
fun interface Decision<T> {
    fun handle(value: T, next: (T) -> T): T
}

class HookKey<T>(val name: String) {
    override fun toString(): String = "HookKey($name)"
}

/** 云同步拉取到的一批远端记录，可在写入 codec 之前合并/改写。 */
data class SyncRecord(
    val key: String,
    val ids: List<String>,
    val data: List<String>,
)

/** 宿主即将展示的提醒通知，可追加课程/考试上下文。 */
data class ReminderNotification(
    val title: String,
    val text: String,
    val priority: Int = Int.MIN_VALUE,
)

object HookKeys {
    val SYNC_PULL_RECORD = HookKey<SyncRecord>("hita.sync.pullRecord")
    val REMINDER_NOTIFY = HookKey<ReminderNotification>("hita.reminder.notify")
}

/** 插件作用域内的 hook 注册与执行入口。 */
interface PluginHooks {
    fun <T> register(key: HookKey<T>, listener: Decision<T>): AutoCloseable

    fun <T> run(key: HookKey<T>, value: T): T
}
