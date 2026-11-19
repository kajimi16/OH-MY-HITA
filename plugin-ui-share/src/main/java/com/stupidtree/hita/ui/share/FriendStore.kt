package com.stupidtree.hita.ui.share

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class FriendTimetable(
    val shareId: String,
    val nickname: String,
    val code: String,
    val importedAt: Long,
)

/** 好友课表口令只保存在插件自己的 SharedPreferences。 */
class FriendStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("friend_timetables", Context.MODE_PRIVATE)

    fun load(): List<FriendTimetable> = runCatching {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val code = item.optString("code")
                val nickname = item.optString("nickname")
                val shareId = item.optString("shareId")
                if (code.isBlank() || shareId.isBlank()) continue
                add(FriendTimetable(shareId, nickname.ifBlank { "好友" }, code, item.optLong("importedAt")))
            }
        }
    }.getOrDefault(emptyList())

    fun remember(shareId: String, nickname: String, code: String): List<FriendTimetable> {
        if (shareId.isBlank() || code.isBlank()) return load()
        val next = (load().filterNot { it.shareId == shareId || it.code == code } +
            FriendTimetable(shareId, nickname.ifBlank { "好友" }, code, System.currentTimeMillis()))
            .take(MAX_FRIENDS)
        save(next)
        return next
    }

    fun remove(shareId: String): List<FriendTimetable> {
        val next = load().filterNot { it.shareId == shareId }
        save(next)
        return next
    }

    private fun save(items: List<FriendTimetable>) {
        val array = JSONArray()
        items.forEach {
            array.put(JSONObject().apply {
                put("shareId", it.shareId)
                put("nickname", it.nickname)
                put("code", it.code)
                put("importedAt", it.importedAt)
            })
        }
        prefs.edit().putString(KEY, array.toString()).commit()
    }

    private companion object {
        const val KEY = "friends"
        const val MAX_FRIENDS = 20
    }
}
