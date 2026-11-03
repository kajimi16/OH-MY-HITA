package com.stupidtree.hitax.plugin

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.stupidtree.hita.api.Plugin
import com.stupidtree.hita.api.PluginContext
import com.stupidtree.hita.api.SyncCodec
import com.stupidtree.hitax.data.AppDatabase
import com.stupidtree.hitax.data.model.GsonBuilderUtil
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.TermSubject
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.data.source.dao.EventItemDao
import com.stupidtree.hitax.data.source.dao.SubjectDao
import com.stupidtree.hitax.data.source.dao.TimetableDao

/**
 * 课表域的三张可同步表。
 *
 * 原先这三个分支是 `HApplication` 里 `when (key) { "timetable" -> ... }` 的硬编码；
 * 现在它们只是三个 [SyncCodec]，宿主只按 key 查表转发。
 * P3 把课表数据层搬进 plugin-hita 之后，这个插件会跟着搬走，宿主一行不用改。
 */
class TimetableSyncPlugin(private val appContext: Context) : Plugin {

    override val id = "com.stupidtree.hitax.sync"

    override val displayName = "课表云同步"

    override fun install(ctx: PluginContext) {
        val db = AppDatabase.getDatabase(appContext)
        ctx.sync.register(TimetableCodec(db.timetableDao()))
        ctx.sync.register(SubjectCodec(db.subjectDao()))
        ctx.sync.register(EventCodec(db.eventItemDao()))
    }
}

private class TimetableCodec(private val dao: TimetableDao) : SyncCodec {

    override val key = "timetable"

    override fun export(ids: List<String>): List<JsonObject> =
        dao.getTimetablesInIdsSync(ids).map { timetable ->
            Gson().toJsonTree(timetable).asJsonObject.apply {
                addProperty("startTime", timetable.startTime.time)
                addProperty("endTime", timetable.endTime.time)
            }
        }

    override fun save(ids: List<String>, data: List<String>) {
        val byId = data
            .map { GsonBuilderUtil.create().fromJson(it, Timetable::class.java) }
            .associateBy { it.id }
        dao.saveTimetablesSync(ids.mapNotNull { byId[it] })
    }

    override fun delete(ids: List<String>) = dao.deleteTimetablesInIdsSync(ids)

    override fun clear() = dao.clear()
}

private class SubjectCodec(private val dao: SubjectDao) : SyncCodec {

    override val key = "subject"

    override fun export(ids: List<String>): List<JsonObject> =
        dao.getSubjectsInIdsSync(ids).map { Gson().toJsonTree(it).asJsonObject }

    override fun save(ids: List<String>, data: List<String>) {
        val byId = data
            .map { GsonBuilderUtil.create().fromJson(it, TermSubject::class.java) }
            .associateBy { it.id }
        dao.saveSubjectsSync(ids.mapNotNull { byId[it] })
    }

    override fun delete(ids: List<String>) = dao.deleteSubjectsInIdsSync(ids)

    override fun clear() = dao.clear()
}

private class EventCodec(private val dao: EventItemDao) : SyncCodec {

    override val key = "event"

    override fun export(ids: List<String>): List<JsonObject> =
        dao.getEventInIdsSync(ids).map { event ->
            Gson().toJsonTree(event).asJsonObject.apply {
                addProperty("from", event.from.time)
                addProperty("to", event.to.time)
            }
        }

    override fun save(ids: List<String>, data: List<String>) {
        val byId = data
            .map { GsonBuilderUtil.create().fromJson(it, EventItem::class.java) }
            .associateBy { it.id }
        dao.saveEvents(ids.mapNotNull { byId[it] })
    }

    override fun delete(ids: List<String>) = dao.deleteEventsInIdsSync(ids)

    override fun clear() = dao.clear()
}
