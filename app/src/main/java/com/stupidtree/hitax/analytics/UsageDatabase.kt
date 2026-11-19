package com.stupidtree.hitax.analytics

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UsageEvent::class], version = 1, exportSchema = false)
abstract class UsageDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao

    companion object {
        @Volatile private var instance: UsageDatabase? = null

        fun get(context: Context): UsageDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    UsageDatabase::class.java,
                    "usage_events.db",
                ).build().also { instance = it }
            }
    }
}
