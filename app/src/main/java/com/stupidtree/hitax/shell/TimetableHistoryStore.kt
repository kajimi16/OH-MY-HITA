package com.stupidtree.hitax.shell

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stupidtree.hitax.data.model.timetable.EventItem
import java.sql.Timestamp
import java.util.UUID

data class TimetableVersionRecord(
    val id: String,
    val revision: Int,
    val committedAt: Long,
    /** INITIAL / MANUAL / RESTORE_BACKUP */
    val reason: String,
    val fingerprint: String,
    val events: List<EventItem>,
)

/**
 * 3.3.x 教务课表版本记录。
 *
 * 仅保存 CLASS 事件；按课表 id 隔离，最多保留 5 条。写入是同步的，
 * 调用方应在 IO 线程调用。
 */
class TimetableHistoryStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val type = object : TypeToken<List<StoredVersion>>() {}.type

    fun versions(timetableId: String): List<TimetableVersionRecord> =
        read(timetableId).map { it.toRecord() }

    /**
     * @return 新增的版本；指纹未变化时返回 null，避免每次刷新都产生重复版本。
     */
    fun record(
        timetableId: String,
        events: List<EventItem>,
        reason: String,
        now: Long = System.currentTimeMillis(),
    ): TimetableVersionRecord? {
        val classEvents = TimetableHistoryDiff.eventsFor(timetableId, events)
        if (classEvents.isEmpty()) return null
        val fingerprint = TimetableHistoryDiff.fingerprint(classEvents)
        synchronized(lock) {
            val existing = read(timetableId)
            if (existing.firstOrNull()?.fingerprint == fingerprint) return null
            val revision = (existing.firstOrNull()?.revision ?: 0) + 1
            val record = TimetableVersionRecord(
                id = UUID.randomUUID().toString(),
                revision = revision,
                committedAt = now,
                reason = reason,
                fingerprint = fingerprint,
                events = classEvents,
            )
            val updated = (listOf(record.toStored()) + existing).take(MAX_VERSIONS)
            prefs.edit().putString(key(timetableId), gson.toJson(updated)).commit()
            return record
        }
    }

    fun replace(timetableId: String, versions: List<TimetableVersionRecord>) {
        synchronized(lock) {
            prefs.edit().putString(
                key(timetableId),
                gson.toJson(versions.take(MAX_VERSIONS).map { it.toStored() }),
            ).commit()
        }
    }

    private fun read(timetableId: String): List<StoredVersion> {
        val raw = prefs.getString(key(timetableId), null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<StoredVersion>>(raw, type).orEmpty()
        }.getOrDefault(emptyList()).filter { it.events.isNotEmpty() }
    }

    private fun key(timetableId: String) = "versions_$timetableId"

    private fun StoredVersion.toRecord(): TimetableVersionRecord = TimetableVersionRecord(
        id = id,
        revision = revision,
        committedAt = committedAt,
        reason = reason,
        fingerprint = fingerprint,
        events = events.map { it.toEvent() },
    )

    private fun TimetableVersionRecord.toStored(): StoredVersion = StoredVersion(
        id = id,
        revision = revision,
        committedAt = committedAt,
        reason = reason,
        fingerprint = fingerprint,
        events = events.map { StoredEvent.from(it) },
    )

    private data class StoredVersion(
        val id: String,
        val revision: Int,
        val committedAt: Long,
        val reason: String,
        val fingerprint: String,
        val events: List<StoredEvent>,
    )

    private data class StoredEvent(
        val id: String,
        val type: String,
        val name: String,
        val place: String?,
        val teacher: String?,
        val subjectId: String,
        val timetableId: String,
        val from: Long,
        val to: Long,
        val fromNumber: Int,
        val lastNumber: Int,
        val createdAt: Long,
    ) {
        fun toEvent(): EventItem = EventItem().apply {
            this.id = this@StoredEvent.id
            this.type = runCatching {
                EventItem.TYPE.valueOf(this@StoredEvent.type)
            }.getOrDefault(EventItem.TYPE.CLASS)
            this.name = this@StoredEvent.name
            this.place = this@StoredEvent.place
            this.teacher = this@StoredEvent.teacher
            this.subjectId = this@StoredEvent.subjectId
            this.timetableId = this@StoredEvent.timetableId
            this.from = Timestamp(this@StoredEvent.from)
            this.to = Timestamp(this@StoredEvent.to)
            this.fromNumber = this@StoredEvent.fromNumber
            this.lastNumber = this@StoredEvent.lastNumber
            this.createdAt = Timestamp(this@StoredEvent.createdAt)
        }

        companion object {
            fun from(event: EventItem): StoredEvent = StoredEvent(
                id = event.id,
                type = event.type.name,
                name = event.name,
                place = event.place,
                teacher = event.teacher,
                subjectId = event.subjectId,
                timetableId = event.timetableId,
                from = event.from.time,
                to = event.to.time,
                fromNumber = event.fromNumber,
                lastNumber = event.lastNumber,
                createdAt = event.createdAt.time,
            )
        }
    }

    private companion object {
        const val PREFS = "timetable_versions"
        const val MAX_VERSIONS = 5
        val lock = Any()
    }
}
