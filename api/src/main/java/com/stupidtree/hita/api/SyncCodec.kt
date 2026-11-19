package com.stupidtree.hita.api

import com.google.gson.JsonObject

/**
 * 一种可同步的数据表。
 *
 * 宿主只按 [key] 查表并转发调用，不再知道「课表 / 课程 / 日程」这些具体类型；
 * 新增一种可同步数据 = 注册一个新的 codec。
 */
interface SyncCodec {
    val key: String

    /** 把 [ids] 对应的数据导出成云端格式。 */
    fun export(ids: List<String>): List<JsonObject>

    /** 用云端数据覆盖 [ids] 对应的本地记录。 */
    fun save(ids: List<String>, data: List<String>)

    fun delete(ids: List<String>)

    fun clear()
}

interface SyncRegistry {
    fun register(codec: SyncCodec)

    fun get(key: String): SyncCodec?

    fun keys(): Set<String>
}
