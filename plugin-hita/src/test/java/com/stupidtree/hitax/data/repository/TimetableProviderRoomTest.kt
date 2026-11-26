package com.stupidtree.hitax.data.repository

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.stupidtree.hitax.data.AppDatabase
import com.stupidtree.hitax.data.model.timetable.Timetable
import java.sql.Timestamp
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * provider 分区在真实 SQLite 上的行为。
 *
 * 覆盖三件事：v2→v3 迁移保留旧行、同 code 多来源互不覆盖、旧接口仍能读到课表。
 * 运行：`./gradlew :plugin-hita:testDebugUnitTest --tests '*TimetableProviderRoomTest'`
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class TimetableProviderRoomTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val migrationDbName = "provider-migration.db"
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        context.deleteDatabase(migrationDbName)
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(migrationDbName)
    }

    @Test
    fun v2ToV3KeepsOldRowsAndLeavesProviderNull() {
        writeV2Database()

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, migrationDbName)
            .addMigrations(*AppDatabase.MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            val row = migrated.timetableDao().getTimetableByIdSync("t-old")
            assertNotNull(row)
            assertEquals("旧课表", row!!.name)
            assertEquals("2025-1", row.code)
            assertNull(row.providerId)
        } finally {
            migrated.close()
        }
    }

    @Test
    fun sameCodeDifferentProvidersStayIndependent() {
        val dao = database.timetableDao()
        dao.saveTimetableSync(timetable("t-sz", "hit.shenzhen", startTime = 100))
        dao.saveTimetableSync(timetable("t-hrb", "hit.harbin", startTime = 200))

        assertEquals(
            "t-sz",
            dao.getTimetableByEasCodeAndProviderSync("2025-1", "hit.shenzhen")?.id,
        )
        assertEquals(
            "t-hrb",
            dao.getTimetableByEasCodeAndProviderSync("2025-1", "hit.harbin")?.id,
        )
        assertNull(dao.getTimetableByEasCodeAndProviderSync("2025-1", "hit.weihai"))
        assertEquals(2, dao.getTimetablesSync().size)
    }

    @Test
    fun oldLookupIgnoresProviderButStaysReadable() {
        val dao = database.timetableDao()
        dao.saveTimetableSync(timetable("t-sz", "hit.shenzhen", startTime = 100))
        dao.saveTimetableSync(timetable("t-hrb", "hit.harbin", startTime = 200))

        // 旧服务按 code 查询，不感知来源，仍能读到课表。
        val legacyRead = dao.getTimetableByEASCodeSync("2025-1")
        assertNotNull(legacyRead)
        assertEquals("2025-1", legacyRead!!.code)
    }

    @Test
    fun legacyRowIsClaimedOnFirstProviderHit() {
        val dao = database.timetableDao()
        dao.saveTimetableSync(timetable("t-legacy", null, startTime = 100))

        val claimed = dao.resolveByEasCodeAndProvider("2025-1", "hit.shenzhen")
        assertEquals("t-legacy", claimed?.id)
        assertEquals("hit.shenzhen", claimed?.providerId)

        // 认领后其他来源不得再命中同一行。
        assertNull(dao.resolveByEasCodeAndProvider("2025-1", "hit.harbin"))
        assertEquals(
            "hit.shenzhen",
            dao.getTimetablesInIdsSync(listOf("t-legacy")).single().providerId,
        )
        // 旧接口仍可读到已认领的行。
        assertEquals("t-legacy", dao.getTimetableByEASCodeSync("2025-1")?.id)
    }

    @Test
    fun blankProviderIsExplicitError() {
        val dao = database.timetableDao()
        try {
            dao.resolveByEasCodeAndProvider("2025-1", "  ")
            throw AssertionError("空白 providerId 必须抛出 IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertEquals("providerId must not be blank", expected.message)
        }
    }

    private fun writeV2Database() {
        val file = context.getDatabasePath(migrationDbName)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            V2_ENTITIES.forEach(db::execSQL)
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table " +
                    "(id INTEGER PRIMARY KEY, identity_hash TEXT)"
            )
            db.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) " +
                    "VALUES (42, '$V2_IDENTITY_HASH')"
            )
            db.execSQL(
                "INSERT INTO timetable " +
                    "(id, name, code, startTime, endTime, createdAt, scheduleStructure) " +
                    "VALUES ('t-old', '旧课表', '2025-1', 100, 200, 50, '[]')"
            )
            db.version = 2
        }
    }

    private fun timetable(
        id: String,
        providerId: String?,
        startTime: Long,
    ): Timetable = Timetable().apply {
        this.id = id
        this.name = id
        this.code = "2025-1"
        this.providerId = providerId
        this.startTime = Timestamp(startTime)
        this.endTime = Timestamp(startTime + 1000)
        this.createdAt = Timestamp(startTime)
    }

    private companion object {
        const val V2_IDENTITY_HASH = "82d0400e20a7950577b794144e026773"

        /** 与 `schemas/.../2.json` 的 createSql 一致；Room 迁移后会校验列集合。 */
        val V2_ENTITIES = listOf(
            "CREATE TABLE IF NOT EXISTS `events` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, `place` TEXT, `teacher` TEXT, `subjectId` TEXT NOT NULL, " +
                "`timetableId` TEXT NOT NULL, `from` INTEGER NOT NULL, `to` INTEGER NOT NULL, " +
                "`fromNumber` INTEGER NOT NULL, `lastNumber` INTEGER NOT NULL, " +
                "`created_at` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `subject` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`timetableId` TEXT NOT NULL, `type` TEXT NOT NULL, `field` TEXT, " +
                "`selectCategory` TEXT, `nature` TEXT, `credit` REAL NOT NULL, `school` TEXT, " +
                "`countInSPA` INTEGER NOT NULL, `code` TEXT, `key` TEXT, " +
                "`createdAt` INTEGER NOT NULL, `color` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `timetable` (`id` TEXT NOT NULL, `name` TEXT, " +
                "`code` TEXT, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `scheduleStructure` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
    }
}
