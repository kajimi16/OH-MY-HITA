package com.stupidtree.hitax.reminder

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stupidtree.hita.api.ReminderSpec

/** 宿主持久化的提醒记录；用于取消和开机恢复。 */
internal data class StoredReminder(
    val ownerId: String,
    val id: String,
    val triggerAtMillis: Long,
    val title: String,
    val text: String,
) {
    fun toSpec() = ReminderSpec(ownerId, id, triggerAtMillis, title, text)
}

internal class ReminderStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val listType = object : TypeToken<List<StoredReminder>>() {}.type

    fun all(): List<StoredReminder> = synchronized(lock) {
        runCatching {
            val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
            gson.fromJson<List<StoredReminder>>(raw, listType) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun upsert(spec: ReminderSpec) = synchronized(lock) {
        val next = all().filterNot { it.ownerId == spec.ownerId && it.id == spec.id } +
            StoredReminder(
                ownerId = spec.ownerId,
                id = spec.id,
                triggerAtMillis = spec.triggerAtMillis,
                title = spec.title,
                text = spec.text,
            )
        save(next)
    }

    fun remove(ownerId: String, id: String) = synchronized(lock) {
        save(all().filterNot { it.ownerId == ownerId && it.id == id })
    }

    fun removeAll(ownerId: String) = synchronized(lock) {
        save(all().filterNot { it.ownerId == ownerId })
    }

    private fun save(items: List<StoredReminder>) {
        prefs.edit().putString(KEY_ITEMS, gson.toJson(items)).apply()
    }

    companion object {
        private const val PREFS_NAME = "plugin_reminders"
        private const val KEY_ITEMS = "items"
        private val lock = Any()
    }
}
