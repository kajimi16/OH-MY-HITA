package com.stupidtree.hita.ui.homework

import android.content.Context
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

data class HomeworkItem(
    val id: String,
    val title: String,
    val createdAt: Long,
    val completed: Boolean = false,
    val plannedDate: String? = null,
)

/** 作业数据只保存在插件自己的 SharedPreferences，不写入宿主数据库。 */
class HomeworkStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("homework_plugin", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun load(): List<HomeworkItem> = runCatching {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        gson.fromJson(raw, Array<HomeworkItem>::class.java)
            .filterNotNull()
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.id }
            .take(MAX_ITEMS)
    }.getOrDefault(emptyList())

    fun add(title: String, plannedDate: String?): List<HomeworkItem> {
        val text = title.trim().take(MAX_TITLE)
        if (text.isEmpty()) return load()
        val items = load()
        if (items.size >= MAX_ITEMS) return items
        val next = items + HomeworkItem(
            id = UUID.randomUUID().toString(),
            title = text,
            createdAt = System.currentTimeMillis(),
            plannedDate = plannedDate,
        )
        save(next)
        return next
    }

    fun setCompleted(id: String, completed: Boolean): List<HomeworkItem> {
        val next = load().map { if (it.id == id) it.copy(completed = completed) else it }
        save(next)
        return next
    }

    fun delete(id: String): List<HomeworkItem> {
        val next = load().filterNot { it.id == id }
        save(next)
        return next
    }

    fun clearCompleted(): List<HomeworkItem> {
        val next = load().filterNot { it.completed }
        save(next)
        return next
    }

    private fun save(items: List<HomeworkItem>) {
        prefs.edit().putString(KEY_ITEMS, gson.toJson(items)).apply()
    }

    companion object {
        private const val KEY_ITEMS = "items"
        private const val MAX_ITEMS = 500
        private const val MAX_TITLE = 200

        fun normalizeDate(raw: String): String? {
            val text = raw.trim()
            if (text.isEmpty()) return null
            return runCatching {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
                val date = format.parse(text) ?: return null
                format.format(date).takeIf { it == text }
            }.getOrNull()
        }
    }
}
