package com.stupidtree.hita.plugin.campusui

import com.stupidtree.hita.api.CampusResult
import com.stupidtree.hita.api.CampusServiceTarget
import com.stupidtree.hita.api.CampusSessionState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 一次查询在界面上的三种状态。
 *
 * [Failed.loginTarget] 非空表示门户缺少会话，需要打开登录覆盖层。
 */
internal sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>

    data class Ready<T>(val value: T) : LoadState<T>

    data class Failed(
        val title: String,
        val message: String,
        val loginTarget: CampusServiceTarget? = null,
    ) : LoadState<Nothing>
}

/** 把契约结果翻译成界面状态；[CampusResult] 的 message 原样保留。 */
internal fun <T> CampusResult<T>.asLoadState(): LoadState<T> = when (this) {
    is CampusResult.Success -> LoadState.Ready(value)
    is CampusResult.NotSignedIn -> LoadState.Failed(
        title = if (target == CampusServiceTarget.CARD || target == CampusServiceTarget.LAB) {
            "需要登录门户"
        } else {
            "${targetLabel(target)}需要登录"
        },
        message = when (target) {
            CampusServiceTarget.CARD ->
                "校园卡数据来自深圳信息门户（dxcdp.hitsz.edu.cn），需要单独完成统一身份认证登录后才会显示。登录在下方页面完成后会自动返回本页。"
            CampusServiceTarget.LAB ->
                "实验中心课表在 lab.hitsz.edu.cn 上，需要先完成该站点登录。登录成功后会返回本页并自动读取本学期实验课。"
            else ->
                "浏览器里没有${targetLabel(target)}的会话 Cookie。完成登录后回到本页点“重试”。"
        },
        loginTarget = target,
    )
    is CampusResult.NetworkError -> LoadState.Failed("网络错误", message)
    is CampusResult.RemoteError -> LoadState.Failed(
        title = "服务返回错误",
        message = if (code.isNullOrBlank()) message else "$message（代码 $code）",
    )
    is CampusResult.ParseError -> LoadState.Failed("响应无法识别", message)
}

/** 写操作失败时的单行说明；成功返回 null。 */
internal fun <T> CampusResult<T>.failureMessageOrNull(): String? {
    val failed = asLoadState() as? LoadState.Failed ?: return null
    return "${failed.title}：${failed.message}"
}

internal fun targetLabel(target: CampusServiceTarget): String = when (target) {
    CampusServiceTarget.CARD -> "校园卡"
    CampusServiceTarget.POD -> "静音仓"
    CampusServiceTarget.LIBRARY -> "图书馆"
    CampusServiceTarget.LAB -> "实验中心"
}

/** 登录覆盖层打开的网页；四个门户各自独立。 */
internal fun portalUrl(target: CampusServiceTarget): String = when (target) {
    CampusServiceTarget.CARD ->
        "https://ids.hit.edu.cn/authserver/login?service=https%3A%2F%2Fdxcdp.hitsz.edu.cn%3A443%2Fportal-api%2Flogin_cas"
    CampusServiceTarget.POD -> "https://www.utszlib.edu.cn/space-reservation-fg/?locale=zh_CN"
    CampusServiceTarget.LIBRARY -> "https://www.utszlib.edu.cn/"
    CampusServiceTarget.LAB -> "https://lab.hitsz.edu.cn/hitszlims/test?PROJECT_NAME=hitszlims"
}

internal fun sessionSignedIn(
    state: CampusSessionState,
    target: CampusServiceTarget,
): Boolean = when (target) {
    CampusServiceTarget.CARD -> state.card
    CampusServiceTarget.POD -> state.pod
    CampusServiceTarget.LIBRARY -> state.library
    CampusServiceTarget.LAB -> state.lab
}

internal val shanghaiZone: TimeZone get() = TimeZone.getTimeZone("Asia/Shanghai")

/** 北京时间当天 `yyyy-MM-dd`。 */
internal fun todayDateText(now: Long = System.currentTimeMillis()): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        .apply { timeZone = shanghaiZone }
        .format(Date(now))

/** 北京时间当天第几分钟。 */
internal fun nowMinutesOfDay(now: Long = System.currentTimeMillis()): Int {
    val calendar = Calendar.getInstance(shanghaiZone).apply { timeInMillis = now }
    return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
}

/** 在 `yyyy-MM-dd` 上按天偏移；解析失败返回原值。 */
internal fun shiftDate(date: String, days: Int): String {
    val calendar = parseDate(date) ?: return date
    calendar.add(Calendar.DAY_OF_MONTH, days)
    return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        .apply { timeZone = shanghaiZone }
        .format(calendar.time)
}

/** 当天分钟数格式化为 `HH:mm`。 */
internal fun formatMinutes(minutes: Int): String =
    String.format(Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60)

/** 时间戳格式化为北京时间 `MM-dd HH:mm`。 */
internal fun formatTimestamp(millis: Long, pattern: String = "MM-dd HH:mm"): String =
    SimpleDateFormat(pattern, Locale.ROOT)
        .apply { timeZone = shanghaiZone }
        .format(Date(millis))

/** 分转元，保留两位小数；负数带负号。 */
internal fun formatYuan(cents: Int): String {
    val sign = if (cents < 0) "-" else ""
    val absolute = if (cents < 0) -cents.toLong() else cents.toLong()
    return "$sign¥${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
}

/** 流水金额带符号：消费为负，入账为正。 */
internal fun formatSignedYuan(cents: Int): String {
    val sign = if (cents < 0) "-" else "+"
    val absolute = if (cents < 0) -cents.toLong() else cents.toLong()
    return "$sign¥${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
}

/** 当前北京时间星期几，`1` 为周一；用于判断实验课是否已上。 */
internal fun todayWeekday(now: Long = System.currentTimeMillis()): Int =
    Calendar.getInstance(shanghaiZone).apply { timeInMillis = now }.get(Calendar.DAY_OF_WEEK)
        .let { if (it == Calendar.SUNDAY) 7 else it - 1 }

private fun parseDate(date: String): Calendar? = runCatching {
    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        .apply { timeZone = shanghaiZone; isLenient = false }
        .parse(date) ?: return null
    Calendar.getInstance(shanghaiZone).apply { time = parsed }
}.getOrNull()
