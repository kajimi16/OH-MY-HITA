package com.stupidtree.hita.api

import android.content.Context

/** 后台任务执行时可用的宿主服务。 */
interface BackgroundTaskHost {
    fun <T : Any> service(key: ServiceKey<T>): T?
}

/**
 * 市场 UI 插件可选实现：宿主后台任务触发时会加载插件类并调用 [run]。
 *
 * 插件没有 manifest，不能自己注册 Worker / Service；调度统一由宿主完成。
 */
interface BackgroundTask {
    /** 返回 false 表示本次失败；宿主仍按任务策略继续或结束。 */
    fun run(context: Context, host: BackgroundTaskHost): Boolean
}

/**
 * 宿主后台调度能力（API 7）。
 *
 * [ownerId] 通常直接填插件 id。周期任务最小间隔 15 分钟；一次性任务可短延迟，
 * 适合“稍后刷新”这类插件内部工作。宿主进程被杀后由 WorkManager 恢复。
 */
interface BackgroundTaskService {

    /** 延迟执行一次；[delayMillis] 必须大于 0。 */
    fun scheduleOnce(ownerId: String, taskId: String, delayMillis: Long): Boolean

    /** 周期执行；[intervalMinutes] 小于 15 会被拒绝。 */
    fun schedulePeriodic(ownerId: String, taskId: String, intervalMinutes: Int): Boolean

    fun cancel(ownerId: String, taskId: String)

    fun cancelAll(ownerId: String)

    companion object {
        val KEY = ServiceKey<BackgroundTaskService>("hita.background")
    }
}
