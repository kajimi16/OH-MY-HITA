package com.stupidtree.hitax.agent.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.stupidtree.hitax.agent.model.ChatMessageEntity
import com.stupidtree.hitax.agent.model.ChatSession

@Database(
    entities = [ChatSession::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun sessionDao(): ChatSessionDao
    abstract fun messageDao(): ChatMessageDao

    companion object {
        @Volatile private var instance: ChatDatabase? = null

        fun get(context: Context): ChatDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                ChatDatabase::class.java,
                "hita_agent_chat",
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
