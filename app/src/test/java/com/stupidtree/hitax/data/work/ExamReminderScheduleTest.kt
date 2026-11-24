package com.stupidtree.hitax.data.work

import com.stupidtree.hitax.data.work.ExamReminderSchedule.Exam
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamReminderScheduleTest {

    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = at(9, 30)
    private val exam = Exam(
        id = "高数|2026-01-10|08:30-10:30",
        name = "高数",
        place = "A101",
        startMillis = at(10, 0),
    )

    @Test
    fun `date parsing accepts padded and unpadded ymd and rejects junk`() {
        assertEquals(2026, ExamReminderSchedule.parseDate("2026-01-10")?.year)
        assertEquals(3, ExamReminderSchedule.parseDate(" 2026-3-9 ")?.monthValue)
        assertNull(ExamReminderSchedule.parseDate("2026/01/10"))
        assertNull(ExamReminderSchedule.parseDate("2026-13-01"))
        assertNull(ExamReminderSchedule.parseDate("2026-02-30"))
        assertNull(ExamReminderSchedule.parseDate(null))
    }

    @Test
    fun `time parsing needs a full range and tolerates separators and spaces`() {
        assertEquals(
            "08:30-10:30",
            ExamReminderSchedule.parseTimeRange(" 08:30 - 10:30 ")?.let { "${it.first}-${it.second}" },
        )
        assertEquals(
            "08:30-10:30",
            ExamReminderSchedule.parseTimeRange("08:30~10:30")?.let { "${it.first}-${it.second}" },
        )
        assertEquals(
            "08:30-10:30",
            ExamReminderSchedule.parseTimeRange("08:30—10:30")?.let { "${it.first}-${it.second}" },
        )
        assertNull(ExamReminderSchedule.parseTimeRange("08:30"))
        assertNull(ExamReminderSchedule.parseTimeRange(null))
    }

    @Test
    fun `time parsing rejects out of range and reversed ranges`() {
        assertNull(ExamReminderSchedule.parseTimeRange("24:00-25:00"))
        assertNull(ExamReminderSchedule.parseTimeRange("08:60-10:30"))
        assertNull(ExamReminderSchedule.parseTimeRange("10:30-08:30"))
    }

    @Test
    fun `start millis combines date and start time in the given zone`() {
        assertEquals(
            at(8, 30),
            ExamReminderSchedule.startMillis("2026-01-10", "08:30-10:30", zone),
        )
        assertNull(ExamReminderSchedule.startMillis("2026-01-10", "08:30", zone))
        assertNull(ExamReminderSchedule.startMillis("bad", "08:30-10:30", zone))
    }

    @Test
    fun `exam key is stable across fetches and keeps end time distinct`() {
        val first = ExamReminderSchedule.examKey("高数", "2026-01-10", "08:30-10:30")
        val again = ExamReminderSchedule.examKey(" 高数 ", " 2026-01-10 ", " 08:30 - 10:30 ")
        assertEquals(first, again)
        assertNotEquals(
            first,
            ExamReminderSchedule.examKey("高数", "2026-01-10", "08:30-10:31"),
        )
        assertNull(ExamReminderSchedule.examKey(null, "2026-01-10", "08:30-10:30"))
        assertNull(ExamReminderSchedule.examKey("高数", "", "08:30-10:30"))
    }

    @Test
    fun `triggers carry a distinct key per lead`() {
        val keys = ExamReminderSchedule.triggers(listOf(exam)).map { it.key }.toSet()
        assertEquals(
            setOf(
                "${exam.id}#start#1440",
                "${exam.id}#start#720",
                "${exam.id}#start#60",
                "${exam.id}#start#30",
            ),
            keys,
        )
        assertEquals(4, keys.size)
    }

    @Test
    fun `due now fires only the most urgent due lead per exam`() {
        val due = ExamReminderSchedule.dueNow(listOf(exam), now, sentKeys = emptySet())
        assertEquals(listOf("${exam.id}#start#30"), due.map { it.key })
    }

    @Test
    fun `due now waits for the window to open`() {
        val before = now - 1
        assertTrue(
            ExamReminderSchedule.dueNow(listOf(exam), before, leads = listOf(30)).isEmpty()
        )
    }

    @Test
    fun `due now skips leads older than the window`() {
        val scheduled = exam.copy(startMillis = at(11, 0))
        val beyond = at(10, 0) + ExamReminderSchedule.DUE_WINDOW_MS + 1
        assertTrue(
            ExamReminderSchedule.dueNow(listOf(scheduled), beyond, leads = listOf(60)).isEmpty()
        )
    }

    @Test
    fun `due now includes the window boundary`() {
        val scheduled = exam.copy(startMillis = at(11, 0))
        val boundary = at(10, 0) + ExamReminderSchedule.DUE_WINDOW_MS
        assertEquals(
            listOf("${exam.id}#start#60"),
            ExamReminderSchedule.dueNow(listOf(scheduled), boundary, leads = listOf(60)).map { it.key },
        )
    }

    @Test
    fun `due now does not remind a started exam`() {
        val started = at(10, 1)
        assertTrue(ExamReminderSchedule.dueNow(listOf(exam), started).isEmpty())
    }

    @Test
    fun `due now deduplicates already sent keys`() {
        val sent = setOf("${exam.id}#start#30")
        assertTrue(ExamReminderSchedule.dueNow(listOf(exam), now, sentKeys = sent).isEmpty())
    }

    @Test
    fun `due now orders multiple exams by fire time`() {
        val later = exam.copy(id = "later", startMillis = at(9, 50))
        val due = ExamReminderSchedule.dueNow(listOf(later, exam), now)
        assertEquals(
            listOf("later#start#30", "${exam.id}#start#30"),
            due.map { it.key },
        )
    }

    @Test
    fun `next due picks the earliest future unsent lead`() {
        val next = ExamReminderSchedule.nextDue(listOf(exam), at(8, 0))
        assertEquals("${exam.id}#start#60", next?.key)
        assertEquals(at(9, 0), next?.fireAtMillis)
    }

    @Test
    fun `next due skips sent and already passed leads`() {
        val sent = setOf("${exam.id}#start#1440", "${exam.id}#start#720")
        assertEquals(
            "${exam.id}#start#60",
            ExamReminderSchedule.nextDue(listOf(exam), at(8, 0), sentKeys = sent)?.key,
        )
        assertEquals(
            "${exam.id}#start#30",
            ExamReminderSchedule.nextDue(listOf(exam), at(9, 1), sentKeys = sent)?.key,
        )
    }

    @Test
    fun `next due returns null when no lead remains`() {
        val allSent = ExamReminderSchedule.triggers(listOf(exam)).map { it.key }.toSet()
        assertNull(ExamReminderSchedule.nextDue(listOf(exam), at(8, 0), sentKeys = allSent))
        assertNull(ExamReminderSchedule.nextDue(listOf(exam.copy(startMillis = at(7, 0))), now))
    }

    @Test
    fun `empty leads do not produce reminders`() {
        assertFalse(ExamReminderSchedule.shouldSchedule(emptyList()))
        assertTrue(ExamReminderSchedule.shouldSchedule(listOf(1440)))
        assertTrue(ExamReminderSchedule.dueNow(listOf(exam), now, leads = emptyList()).isEmpty())
        assertNull(ExamReminderSchedule.nextDue(listOf(exam), now, leads = emptyList()))
    }

    private fun at(hour: Int, minute: Int): Long =
        ZonedDateTime.of(2026, 1, 10, hour, minute, 0, 0, zone)
            .toInstant()
            .toEpochMilli()
}
