package com.stupidtree.hitax.data.work

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 考试提醒的纯计算层：日期/时间解析、固定档位、到期窗口与去重键。
 *
 * 不依赖 Android，也不读写任何状态；Worker 只负责取数、发通知和持久化。
 */
object ExamReminderSchedule {

    /** 与 iOS `fixedExamReminderDates` 一致：考前 24h / 12h / 1h / 30m。 */
    val DEFAULT_LEADS_MINUTES: List<Int> = listOf(24 * 60, 12 * 60, 60, 30)

    /** 只补发最近这段时间内到期的档位；更早的档位视为错过，不再打扰。 */
    const val DUE_WINDOW_MS: Long = 30L * 60L * 1000L

    const val START_PHASE: String = "start"

    private val DATE_PATTERN = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})$""")
    private val TIME_RANGE_PATTERN =
        Regex("""^(\d{1,2}):(\d{2})\s*[-~—–－]\s*(\d{1,2}):(\d{2})$""")
    private val KEY_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

    /** 一条考试的可计算摘要。id 由 [examKey] 生成，跨多次拉取保持稳定。 */
    data class Exam(
        val id: String,
        val name: String,
        val place: String,
        val startMillis: Long,
    )

    /**
     * 一条待发提醒。
     *
     * key 形如 `高数|2026-01-10|08:30-10:30#start#1440`：同一场考试的不同档位互不吞掉，
     * 同一档位只发一次。
     */
    data class Trigger(
        val examId: String,
        val leadMinutes: Int,
        val fireAtMillis: Long,
    ) {
        val key: String get() = "$examId#$START_PHASE#$leadMinutes"
    }

    fun shouldSchedule(leads: List<Int>): Boolean = leads.isNotEmpty()

    /** 解析 `yyyy-MM-dd`；非法或缺失返回 null。 */
    fun parseDate(raw: String?): LocalDate? {
        val match = DATE_PATTERN.matchEntire(raw?.trim().orEmpty()) ?: return null
        val (year, month, day) = match.destructured
        return runCatching {
            LocalDate.of(year.toInt(), month.toInt(), day.toInt())
        }.getOrNull()
    }

    /**
     * 解析 `HH:mm-HH:mm`，返回开始与结束时间。
     *
     * 只接受完整区间：只有开始时间的记录无法确定考试时段，按非法处理。
     * 分隔符允许 `-`、`~` 与常见全角破折号，两侧空白忽略。
     */
    fun parseTimeRange(raw: String?): Pair<LocalTime, LocalTime>? {
        val match = TIME_RANGE_PATTERN.matchEntire(raw?.trim().orEmpty()) ?: return null
        val (startHour, startMinute, endHour, endMinute) = match.destructured
        val start = safeTime(startHour, startMinute) ?: return null
        val end = safeTime(endHour, endMinute) ?: return null
        if (end.isBefore(start)) return null
        return start to end
    }

    /** 解析考试开始时刻；日期或时间任一非法返回 null。 */
    fun startMillis(
        examDate: String?,
        examTime: String?,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        val date = parseDate(examDate) ?: return null
        val (start, _) = parseTimeRange(examTime) ?: return null
        return date.atTime(start).atZone(zone).toInstant().toEpochMilli()
    }

    /**
     * 稳定的考试标识。
     *
     * [com.stupidtree.hitax.data.model.eas.ExamItem.memoId] 每次拉取都会随机生成，
     * 不能用于去重；课程名 + 日期 + 时间区间足以区分同一批次里的同一场考试。
     * 必填字段缺失时返回 null。
     */
    fun examKey(courseName: String?, examDate: String?, examTime: String?): String? {
        val name = courseName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val date = parseDate(examDate) ?: return null
        val (start, end) = parseTimeRange(examTime) ?: return null
        return "$name|$date|${start.format(KEY_TIME_FORMAT)}-${end.format(KEY_TIME_FORMAT)}"
    }

    /** 展开所有档位的候选提醒，不做过期过滤。 */
    fun triggers(
        exams: List<Exam>,
        leads: List<Int> = DEFAULT_LEADS_MINUTES,
    ): List<Trigger> {
        if (!shouldSchedule(leads)) return emptyList()
        return exams.flatMap { exam ->
            leads.map { lead ->
                Trigger(
                    examId = exam.id,
                    leadMinutes = lead,
                    fireAtMillis = exam.startMillis - lead * 60_000L,
                )
            }
        }
    }

    /**
     * 此刻该发的提醒：考试尚未开始、档位已经到期、到期时间在 [DUE_WINDOW_MS] 内、且未发送过。
     *
     * 同一场考试在同一轮里最多返回一条：检查链路恢复后可能同时跨过多个档位，
     * 只发最接近开考的那一档，避免一次弹出「还有 1 小时」和「还有 30 分钟」两条。
     * 超过窗口的档位不补发，补发「考试还有 24 小时」只会误导。
     */
    fun dueNow(
        exams: List<Exam>,
        now: Long,
        leads: List<Int> = DEFAULT_LEADS_MINUTES,
        sentKeys: Set<String> = emptySet(),
    ): List<Trigger> {
        if (!shouldSchedule(leads)) return emptyList()
        return triggers(exams.filter { it.startMillis > now }, leads)
            .filter { trigger ->
                trigger.fireAtMillis <= now &&
                    now - trigger.fireAtMillis <= DUE_WINDOW_MS
            }
            .groupBy { it.examId }
            .values
            .mapNotNull { due ->
                due.minWithOrNull(
                    compareBy<Trigger> { it.leadMinutes }.thenBy { it.fireAtMillis },
                )
            }
            .filter { it.key !in sentKeys }
            .sortedWith(compareBy<Trigger> { it.fireAtMillis }.thenBy { it.key })
    }

    /** 下一次检查时刻：未发送且尚未到期的最近档位；没有则返回 null。 */
    fun nextDue(
        exams: List<Exam>,
        now: Long,
        leads: List<Int> = DEFAULT_LEADS_MINUTES,
        sentKeys: Set<String> = emptySet(),
    ): Trigger? {
        if (!shouldSchedule(leads)) return null
        return triggers(exams.filter { it.startMillis > now }, leads)
            .filter { it.fireAtMillis > now && it.key !in sentKeys }
            .minWithOrNull(compareBy<Trigger> { it.fireAtMillis }.thenBy { it.key })
    }

    private fun safeTime(hourText: String, minuteText: String): LocalTime? {
        val hour = hourText.toIntOrNull() ?: return null
        val minute = minuteText.toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalTime.of(hour, minute)
    }
}
