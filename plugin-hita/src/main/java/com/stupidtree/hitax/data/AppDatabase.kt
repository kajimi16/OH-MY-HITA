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
         * v2 → v3：课表增加来源分区列，并把已有的教务课表归到深圳教务。
         *
         * 判定依据是 code：非空且去空白后仍非空的课表来自教务导入，归 `hit.shenzhen`；
         * 手工课表 code 为空，保持 providerId 为 NULL。迁移是唯一写来源的时机，
         * 读取路径不再认领或补写来源。
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE timetable ADD COLUMN providerId TEXT")
                db.execSQL(
                    "UPDATE timetable SET providerId = 'hit.shenzhen' " +
                        "WHERE providerId IS NULL AND code IS NOT NULL " +
                        "AND length(trim(code)) > 0"
                )
            }
        }

        /** 迁移链；供 Room 构建与迁移测试共用。 */
        internal val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}
