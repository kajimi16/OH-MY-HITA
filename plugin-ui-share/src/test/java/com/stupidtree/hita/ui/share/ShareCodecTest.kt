package com.stupidtree.hita.ui.share

import com.stupidtree.hita.ui.share.protocol.ShareMode
import com.stupidtree.hita.ui.share.protocol.SharedOccurrence
import com.stupidtree.hita.ui.share.protocol.SharedPeriod
import com.stupidtree.hita.ui.share.protocol.SharedTerm
import com.stupidtree.hita.ui.share.protocol.SharedTimetable
import com.stupidtree.hita.ui.share.protocol.TimetableShareCodec
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.data.model.timetable.TimeInDay
import com.stupidtree.hitax.data.model.timetable.TimePeriodInDay
import com.stupidtree.hitax.data.model.timetable.Timetable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.Timestamp

class ShareCodecTest {

    @Test
    fun encodeDecodeRoundTrip() {
        val start = 1_767_225_600_000L // 2026-01-01 Asia/Shanghai
        val snapshot = SharedTimetable(
            shareId = "11111111-1111-1111-1111-111111111111",
            nickname = "测试",
            mode = ShareMode.FULL,
            term = SharedTerm("课表", "学期", start, start + 120L * 86400000L),
            periods = listOf(SharedPeriod(480, 540), SharedPeriod(540, 600)),
            courses = listOf("高等数学"),
            occurrences = listOf(SharedOccurrence(start + 8 * 3600_000L, start + 9 * 3600_000L, 0, "A101")),
        )
        val codec = TimetableShareCodec()
        val token = codec.encode(snapshot)
        assertTrue(token.startsWith("HITA1:"))
        val decoded = codec.decodeMessage(token)
        assertEquals(snapshot.shareId, decoded.shareId)
        assertEquals(snapshot.courses, decoded.courses)
        assertEquals(snapshot.occurrences.size, decoded.occurrences.size)
    }

    @Test
    fun mapperBuildsBundleFromTimetable() {
        val start = 1_767_225_600_000L
        val timetable = Timetable().apply {
            id = "t1"
            name = "课表"
            startTime = Timestamp(start)
            endTime = Timestamp(start + 120L * 86400000L)
            scheduleStructure = listOf(
                TimePeriodInDay(TimeInDay(8, 0), TimeInDay(9, 0)),
                TimePeriodInDay(TimeInDay(9, 0), TimeInDay(10, 0)),
            )
        }
        val event = EventItem().apply {
            id = "e1"
            timetableId = "t1"
            subjectId = "s1"
            name = "高等数学"
            place = "A101"
            type = EventItem.TYPE.CLASS
            from = Timestamp(start + 8 * 3600_000L)
            to = Timestamp(start + 9 * 3600_000L)
        }
        val snapshot = ShareMapper.snapshot(timetable, listOf(event), "测试")
        val bundle = ShareMapper.bundle(snapshot)
        assertEquals("课表", bundle.timetable.name)
        assertEquals(listOf("高等数学"), bundle.subjects.map { it.name })
        assertEquals(1, bundle.events.size)
        assertEquals("A101", bundle.events[0].place)

        // 导入后的 bundle 再导出，验证同一课表可 round-trip。
        val reExported = ShareMapper.snapshot(bundle.timetable, bundle.events, "测试")
        val decoded = TimetableShareCodec().decodeMessage(TimetableShareCodec().encode(reExported))
        assertEquals(listOf("高等数学"), decoded.courses)
        assertEquals(1, decoded.occurrences.size)
        assertEquals("A101", decoded.occurrences[0].place)
    }
}
