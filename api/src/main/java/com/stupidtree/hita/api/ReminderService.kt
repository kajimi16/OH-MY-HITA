package com.stupidtree.hita.api

/**
 * 插件提交给宿主的一条提醒。
 *
 * [ownerId] 用于隔离撤销范围，通常直接填插件 id；[id] 在 owner 内唯一。
 * 标题/正文由插件提供，宿主负责通知渠道、精确闹钟降级和重启恢复。
 */
data class ReminderSpec(
    val ownerId: String,
    val id: String,
    val triggerAtMillis: Long,
    val title: String,
    val text: String,
)

/**
 * 宿主提醒能力（API 7）。
 *
 * 插件不能自己声明 BroadcastReceiver / 前台服务，因此提醒由宿主统一调度。
 * 返回值只表示“是否已被宿主接受”，不保证系统一定准时送达；精确闹钟被禁用时
 * 宿主会自动降级为非精确闹钟。
 */
interface ReminderService {

    /** 当前是否允许精确闹钟；false 时 [schedule] 仍可用，但可能延迟。 */
    fun canScheduleExactAlarms(): Boolean

    /** 通知权限是否已授予；未授予时 [schedule] 返回 false。 */
    fun canPostNotifications(): Boolean

    /**
     * 安排或覆盖一条提醒。时间已过去、字段为空或没有通知权限时返回 false。
     * 相同 [ReminderSpec.ownerId] + [ReminderSpec.id] 会覆盖旧提醒。
     */
    fun schedule(spec: ReminderSpec): Boolean

    fun cancel(ownerId: String, id: String)

    fun cancelAll(ownerId: String)

    companion object {
        val KEY = ServiceKey<ReminderService>("hita.reminder")
    }
}
