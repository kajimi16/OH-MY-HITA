package com.stupidtree.hitax.analytics

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UsageDao {
    @Insert
    fun insert(event: UsageEvent)

    @Query("SELECT * FROM usage_event ORDER BY timestamp ASC LIMIT :limit")
    fun pending(limit: Int): List<UsageEvent>

    @Query("DELETE FROM usage_event WHERE id IN (:ids)")
    fun delete(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM usage_event")
    fun count(): Int

    @Query("DELETE FROM usage_event")
    fun clear()
}
