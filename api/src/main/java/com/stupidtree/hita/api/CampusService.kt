package com.stupidtree.hita.api

/**
 * 校园扩展数据契约（API 11，由 plugin-campus 提供）。
 *
 * 覆盖四个独立门户：校园卡、静音仓（空间预约）、图书馆智慧墙、实验中心课表。
 * 四个门户各有自己的会话，插件只暴露会话状态，不包含 WebView、Activity 或登录界面。
 * 所有查询都是显式 suspend 方法；失败按 [CampusResult] 分类，不抛异常给调用方。
 *
 * 线程约定：方法内部切到 IO，返回时不在主线程做网络；调用方自行决定回主线程。
 */
interface CampusService {

    /** 当前可用的会话状态。只表示 Cookie 是否存在，不做网络探测。 */
    fun session(): CampusSessionState

    /** 校园卡余额、状态与昨日/本周/本月消费汇总。消费汇总失败时 [CampusCardSummary.spending] 为 null。 */
    suspend fun cardSummary(): CampusResult<CampusCardSummary>

    /** 校园卡消费流水。分页从 1 开始；[range] 决定服务端的时间范围。 */
    suspend fun cardTransactions(
        page: Int = 1,
        pageSize: Int = 20,
        range: CampusCardHistoryRange = CampusCardHistoryRange.MONTH,
    ): CampusResult<CampusCardTransactions>

    /** 指定日期的静音仓空间，按分类分组。日期缺省为北京时间当日。 */
    suspend fun podSpaces(date: String? = null): CampusResult<List<CampusPodGroup>>

    /** 我的静音仓预约记录。 */
    suspend fun podReservations(): CampusResult<List<CampusPodReservation>>

    /** 创建静音仓预约；返回服务端分配的预约 id，成功但无 id 时为 null。 */
    suspend fun createPodReservation(request: CampusPodReservationRequest): CampusResult<Int?>

    /** 取消一条预约。 */
    suspend fun cancelPodReservation(reservationId: Int, reason: String? = null): CampusResult<Unit>

    /** 已保存的同行成员；多人仓预约前先取列表，[CampusPodContacts.defaultMobile] 可直接预填手机号。 */
    suspend fun podContacts(): CampusResult<CampusPodContacts>

    /** 按读者证号核验姓名，添加同行成员前确认没输错人；查不到人时成功结果为 null。 */
    suspend fun verifyPodReader(cardNo: String): CampusResult<CampusPodReader?>

    /**
     * 保存同行成员并返回服务端分配的 id；多人仓预约时把 id 放进 [CampusPodReservationRequest.contactIds]。
     *
     * [mobile] 是可选手机号，只在非空时随创建请求提交；上游联系人接口不保证回读该字段。
     */
    suspend fun addPodContact(
        name: String,
        cardNo: String,
        mobile: String? = null,
    ): CampusResult<CampusPodContact>

    /** 删除已保存的同行成员。 */
    suspend fun removePodContact(id: Int): CampusResult<Unit>

    /** 图书馆智慧墙快照：在馆人数与静音仓占用。 */
    suspend fun librarySmartWall(): CampusResult<CampusLibrarySnapshot>

    /** 本学期实验课进度。 */
    suspend fun labProgress(): CampusResult<CampusLabProgress>

    /**
     * 可提交的开始时间（当天第几分钟），纯计算，不联网。
     *
     * 与 [podSpaces] 返回的 [CampusPodSpace] 配合使用；[nowMinutes] 是调用时刻的当天分钟数。
     */
    fun podStartOptions(space: CampusPodSpace, nowMinutes: Int): List<Int>

    /** 给定开始时间可选的结束时间（当天第几分钟），纯计算，不联网。 */
    fun podEndOptions(space: CampusPodSpace, startMinutes: Int): List<Int>

    companion object {
        val KEY = ServiceKey<CampusService>("hita.campus")
    }
}

/** 四个独立门户。失败结果用它指出是哪一个需要登录。 */
enum class CampusServiceTarget { CARD, POD, LIBRARY, LAB }

/** 会话状态；true 仅表示浏览器里存在该门户的会话 Cookie。 */
data class CampusSessionState(
    val card: Boolean = false,
    val pod: Boolean = false,
    val library: Boolean = false,
    val lab: Boolean = false,
)

/** 校园扩展查询的统一结果。 */
sealed interface CampusResult<out T> {

    data class Success<T>(val value: T) : CampusResult<T>

    /** 缺少该门户的会话；调用方应引导用户完成浏览器登录。 */
    data class NotSignedIn(val target: CampusServiceTarget) : CampusResult<Nothing>

    /** 连接失败、超时或网关不可用。 */
    data class NetworkError(val message: String) : CampusResult<Nothing>

    /** 门户返回了可识别的业务错误。[code] 是服务端错误码，可能为空。 */
    data class RemoteError(val message: String, val code: String? = null) : CampusResult<Nothing>

    /** 门户响应结构无法识别。 */
    data class ParseError(val message: String) : CampusResult<Nothing>
}

/** 校园卡余额与状态；金额单位为分。 */
data class CampusCardSummary(
    val balanceCents: Int,
    val statusText: String? = null,
    val processedAtText: String? = null,
    val spending: CampusCardSpending? = null,
)

/** 昨日 / 本周 / 本月消费汇总，保留门户原文。 */
data class CampusCardSpending(
    val yesterdayText: String? = null,
    val weekText: String? = null,
    val monthText: String? = null,
)

/** 一条消费流水；[amountCents] 保留原始符号（消费为负、入账为正）。 */
data class CampusCardTransaction(
    val id: String,
    val amountCents: Int,
    val occurredAtText: String,
    val occurredAtMillis: Long? = null,
    val merchant: String = "",
    val method: String = "",
)

/** 一页消费流水。 */
data class CampusCardTransactions(
    val items: List<CampusCardTransaction> = emptyList(),
    val total: Int = 0,
)

/** 流水时间范围，枚举顺序固定。 */
enum class CampusCardHistoryRange { MONTH, YEAR, ALL }

/** 静音仓分类；[spaceTypeId] 是服务端分类 id。 */
enum class CampusPodCategory(val spaceTypeId: Int) {
    QUAD_LOCKED(40),
    QUAD(42),
    SINGLE(41),
}

/** 单个静音仓空间及其当日可约时间轴。 */
data class CampusPodSpace(
    val id: Int,
    val codeName: String,
    val areaFullName: String,
    val spaceTypeName: String? = null,
    val category: CampusPodCategory,
    val capacity: Int,
    val teamCapacity: Int,
    val isTeam: Boolean,
    val isAccessControl: Boolean,
    /** 服务端原始 `HH:mm-HH:mm` 区间。 */
    val allowDurations: List<String> = emptyList(),
    /** 服务端标记为不可约的区间，原始 `HH:mm-HH:mm`。 */
    val disabledDurations: List<String> = emptyList(),
    val minDurationMinutes: Int? = null,
    val maxDurationMinutes: Int? = null,
) {
    /** 多人仓必须选的同行人数（不含本人）；单人仓为 null。 */
    val requiredGuestRange: IntRange?
        get() {
            if (!isTeam) return null
            val minGuests = maxOf(1, teamCapacity - 1)
            val maxGuests = maxOf(minGuests, capacity - 1)
            return minGuests..maxGuests
        }
}

/** 一个分类下的空间，用于页面分组。 */
data class CampusPodGroup(
    val category: CampusPodCategory,
    val spaces: List<CampusPodSpace> = emptyList(),
)

/** 一条个人预约记录，只含展示字段。 */
data class CampusPodReservation(
    val id: Int,
    val spaceName: String = "",
    val areaName: String = "",
    val spaceTypeName: String? = null,
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val typeText: String? = null,
    val status: String = "",
    val statusText: String = "",
    val cancelReason: String? = null,
    val isMain: Boolean = false,
    val isNeedSign: Boolean = false,
    val isAllowCancel: Boolean = false,
)

/** 已保存的同行成员（多人静音仓预约时需要）。 */
data class CampusPodContact(
    val id: Int,
    val name: String,
    val cardNo: String,
)

/** 读者核验结果：添加同行成员前先用证号查姓名，确认没输错人。 */
data class CampusPodReader(
    val name: String,
    val cardNo: String,
)

/**
 * 同行成员列表。
 *
 * [defaultMobile] 来自服务端 `other.mobile`，用于预填手机号，可能为空。
 */
data class CampusPodContacts(
    val contacts: List<CampusPodContact> = emptyList(),
    val defaultMobile: String? = null,
)

/**
 * 创建预约的请求。
 *
 * [date] 为 `yyyy-MM-dd`；[startTime] / [endTime] 为 `HH:mm`；
 * [contactIds] 是多人仓必填的同行成员 id。
 */
data class CampusPodReservationRequest(
    val spaceId: Int,
    val date: String,
    val startTime: String,
    val endTime: String,
    val notes: String = "",
    val contactIds: List<Int> = emptyList(),
)

/** 图书馆智慧墙一次抓取的完整快照。 */
data class CampusLibrarySnapshot(
    val currentInLibrary: Int,
    val todayVisitors: Int,
    val occupiedRooms: Int,
    val availableRooms: Int,
    val fetchedAtMillis: Long,
)

/** 实验中心的一节课；[id] 已包含周次，可直接作为列表 key。 */
data class CampusLabSession(
    val id: String,
    val courseName: String,
    val week: Int,
    val weekday: Int,
    val startSection: Int,
    val endSection: Int,
    val room: String? = null,
)

/** 本学期实验课进度。 */
data class CampusLabProgress(
    val termName: String? = null,
    /** 当前教学周；null 表示页面没有给出，无法判断「已上」。 */
    val currentWeek: Int? = null,
    val sessions: List<CampusLabSession> = emptyList(),
    val fetchedAtMillis: Long = 0L,
) {
    val totalCount: Int get() = sessions.size

    /** 已经上过的场次：周次更早，或同一周里星期更早。 */
    fun completedCount(currentWeek: Int, weekday: Int): Int =
        sessions.count { session ->
            session.week < currentWeek ||
                (session.week == currentWeek && session.weekday < weekday)
        }
}
