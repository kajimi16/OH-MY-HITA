package com.stupidtree.hitax.livecourse

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stupidtree.hita.api.LiveCourseSpec

internal class LiveCourseStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("live_courses", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val type = object : TypeToken<List<LiveCourseSpec>>() {}.type

    fun all(): List<LiveCourseSpec> = synchronized(lock) {
        runCatching {
            val raw = prefs.getString(KEY, null) ?: return emptyList()
            gson.fromJson<List<LiveCourseSpec>>(raw, type) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun upsert(spec: LiveCourseSpec) = synchronized(lock) {
        save(all().filterNot { it.ownerId == spec.ownerId && it.id == spec.id } + spec)
    }

    fun remove(ownerId: String, id: String) = synchronized(lock) {
        save(all().filterNot { it.ownerId == ownerId && it.id == id })
    }

    fun removeAll(ownerId: String) = synchronized(lock) {
        save(all().filterNot { it.ownerId == ownerId })
    }

    private fun save(items: List<LiveCourseSpec>) {
        prefs.edit().putString(KEY, gson.toJson(items)).apply()
    }

    companion object {
        private const val KEY = "items"
        private val lock = Any()
    }
}
