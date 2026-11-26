package com.stupidtree.hita.api

import androidx.annotation.WorkerThread
import com.stupidtree.hitax.data.model.timetable.Timetable

/**
 * 课表按来源分区的可选能力（API 14）。
 *
 * 与 [TimetableService] 分开声明，不给已发布的实现类增加抽象方法：
 * 调用方用 [KEY] 探测；取不到时退回 [TimetableService] 的按 code 查询（忽略来源）。
 *
 * 契约：
 * - [getTimetableByEasCodeSync] 只返回 providerId 等于入参的行。
 * - providerId 为 NULL 的行（手工课表或未迁移数据）不命中；读取不补写来源。
 * - 旧教务课表的来源由数据库迁移一次性写入，不在这里认领。
 * - 不返回其他来源的行；找不到返回 null，不抛异常。
 * - 空白 code 或 providerId 是非法输入，抛出 IllegalArgumentException。
 * - 阻塞调用，必须在工作线程执行。
 */
interface TimetableProviderService {

    @WorkerThread
    fun getTimetableByEasCodeSync(easCode: String, providerId: String): Timetable?

    companion object {
        val KEY = ServiceKey<TimetableProviderService>("hita.timetable.provider")
    }
}
