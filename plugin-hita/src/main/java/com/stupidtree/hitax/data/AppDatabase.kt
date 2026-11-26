package com.stupidtree.hitax.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stupidtree.stupiduser.data.model.UserProfile
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.TermSubject
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.data.source.dao.EventItemDao
import com.stupidtree.hitax.data.source.dao.SubjectDao
import com.stupidtree.hitax.data.source.dao.TimetableDao
import com.stupidtree.stupiduser.data.source.dao.UserProfileDao

@Database(
    entities = [EventItem::class, TermSubject::class, Timetable::class],
    version = 3
)
@androidx.room.TypeConverters(TypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventItemDao(): EventItemDao
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableDao(): TimetableDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        @JvmStatic
        fun getDatabase(context: Context): AppDatabase {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java, "hita"
                        ).addMigrations(*MIGRATIONS).build()
                    }
                }
            }
            return INSTANCE!!
        }

        /** 关闭并丢弃单例；清空数据后必须调用，否则会拿到已关闭的库。 */
        @JvmStatic
        fun reset() {
            synchronized(AppDatabase::class.java) {
                runCatching { INSTANCE?.close() }
                INSTANCE = null
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE subject ADD COLUMN selectCategory TEXT")
                db.execSQL("ALTER TABLE subject ADD COLUMN nature TEXT")
            }
        }

        /**
         * v2 → v3：课表增加来源分区列。
         *
         * 旧行 providerId 为 NULL，首次按来源命中时由 DAO 就地补写；
         * 迁移本身只加列，不改写、不删除任何旧行。
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE timetable ADD COLUMN providerId TEXT")
            }
        }

        /** 迁移链；供 Room 构建与迁移测试共用。 */
        internal val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}
