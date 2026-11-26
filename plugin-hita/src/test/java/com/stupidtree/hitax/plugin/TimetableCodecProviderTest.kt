package com.stupidtree.hitax.plugin

import com.stupidtree.hitax.data.model.GsonBuilderUtil
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.data.source.dao.TimetableDao
import java.lang.reflect.Proxy
import java.sql.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 课表同步编解码的 provider 保留规则。
 *
 * 旧客户端不上传 providerId；写入时不得把本机已有来源清空。
 */
class TimetableCodecProviderTest {

    @Test
    fun saveKeepsLocalProviderWhenPayloadOmitsIt() {
        val existing = timetable(id = "t1", providerId = "hit.shenzhen")
        val dao = FakeTimetableDao(listOf(existing))
        val incoming = timetable(id = "t1", providerId = null)

        TimetableCodec(dao.proxy()).save(listOf("t1"), listOf(payload(incoming)))

        assertEquals(1, dao.saved.size)
        assertEquals("hit.shenzhen", dao.saved.single().providerId)
    }

    @Test
    fun saveKeepsExplicitProviderFromPayload() {
        val existing = timetable(id = "t1", providerId = "hit.shenzhen")
        val dao = FakeTimetableDao(listOf(existing))
        val incoming = timetable(id = "t1", providerId = "hit.harbin")

        TimetableCodec(dao.proxy()).save(listOf("t1"), listOf(payload(incoming)))

        assertEquals("hit.harbin", dao.saved.single().providerId)
    }

    @Test
    fun exportCarriesProviderId() {
        val dao = FakeTimetableDao(listOf(timetable(id = "t1", providerId = "hit.shenzhen")))

        val exported = TimetableCodec(dao.proxy()).export(listOf("t1"))

        assertEquals(1, exported.size)
        assertEquals("hit.shenzhen", exported.single().get("providerId").asString)
    }

    private fun payload(timetable: Timetable): String =
        GsonBuilderUtil.create().toJson(timetable)

    private fun timetable(id: String, providerId: String?): Timetable = Timetable().apply {
        this.id = id
        this.name = id
        this.code = "2025-1"
        this.providerId = providerId
        this.startTime = Timestamp(100)
        this.endTime = Timestamp(200)
        this.createdAt = Timestamp(50)
    }

    /** 只实现编解码用到的两个 DAO 方法，避免为测试造整套 Room。 */
    private class FakeTimetableDao(private val existing: List<Timetable>) {
        var saved: List<Timetable> = emptyList()

        fun proxy(): TimetableDao = Proxy.newProxyInstance(
            TimetableDao::class.java.classLoader,
            arrayOf(TimetableDao::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getTimetablesInIdsSync" -> {
                    val ids = (args!![0] as List<*>).filterIsInstance<String>().toSet()
                    existing.filter { it.id in ids }
                }

                "saveTimetablesSync" -> {
                    saved = (args!![0] as List<*>).filterIsInstance<Timetable>().toList()
                    assertTrue(saved.isNotEmpty())
                    null
                }

                else -> throw UnsupportedOperationException(method.name)
            }
        } as TimetableDao
    }
}
