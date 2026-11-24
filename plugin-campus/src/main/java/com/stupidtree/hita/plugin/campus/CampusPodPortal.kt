package com.stupidtree.hita.plugin.campus

import com.stupidtree.hita.api.CampusPodCategory
import com.stupidtree.hita.api.CampusPodContact
import com.stupidtree.hita.api.CampusPodContacts
import com.stupidtree.hita.api.CampusPodGroup
import com.stupidtree.hita.api.CampusPodReader
import com.stupidtree.hita.api.CampusPodReservation
import com.stupidtree.hita.api.CampusPodReservationRequest
import com.stupidtree.hita.api.CampusPodSpace
import com.stupidtree.hita.api.CampusResult
import com.stupidtree.hita.api.CampusServiceTarget
import com.stupidtree.hita.plugin.campus.model.SilentPodSlotPlanner
import com.stupidtree.hita.plugin.campus.source.SilentPodClient
import com.stupidtree.hita.plugin.campus.source.SilentPodException
import com.stupidtree.hita.plugin.campus.source.SilentPodFailureKind
import com.stupidtree.hita.plugin.campus.source.SilentPodParser
import com.stupidtree.hita.plugin.campus.source.SilentPodSessionPolicy

/** 大学城图书馆空间预约：空间、预约与写操作，以及纯本地选时计算。 */
internal class CampusPodPortal(
    private val cookies: CampusCookieStore,
    private val client: SilentPodClient = SilentPodClient(),
) {

    suspend fun spaces(date: String?): CampusResult<List<CampusPodGroup>> =
        campusQuery(CampusServiceTarget.POD) {
            val connection = connectionOrThrow()
            val spaces = client.fetchSpaces(
                cookieHeader = connection.cookie,
                baseUrl = connection.baseUrl,
                date = date ?: client.today(),
            )
            SilentPodParser.group(spaces).map { it.toPublic() }
        }

    suspend fun reservations(): CampusResult<List<CampusPodReservation>> =
        campusQuery(CampusServiceTarget.POD) {
            val connection = connectionOrThrow()
            client.fetchReservations(connection.cookie, connection.baseUrl)
                .map { it.toPublic() }
        }

    suspend fun create(request: CampusPodReservationRequest): CampusResult<Int?> =
        campusQuery(CampusServiceTarget.POD) {
            val connection = connectionOrThrow()
            client.createReservation(
                cookieHeader = connection.cookie,
                spaceId = request.spaceId,
                date = request.date,
                startTime = request.startTime,
                endTime = request.endTime,
                notes = request.notes,
                contactIds = request.contactIds,
                baseUrl = connection.baseUrl,
            )
        }

    suspend fun cancel(reservationId: Int, reason: String?): CampusResult<Unit> =
        campusQuery(CampusServiceTarget.POD) {
            val connection = connectionOrThrow()
            client.cancelReservation(
                cookieHeader = connection.cookie,
                reservationId = reservationId,
                reason = reason.orEmpty(),
                baseUrl = connection.baseUrl,
            )
        }

    suspend fun contacts(): CampusResult<CampusPodContacts> =
        campusQuery(CampusServiceTarget.POD) {
            val connection = connectionOrThrow()
            client.fetchContacts(connection.cookie, connection.baseUrl).toPublic()
        }

    /**
     * 读者核验。
     *
     * 服务端查不到人时返回业务错误 `NO_EXISTS`；对调用方这等价于「没有此人」，
     * 转成成功结果里的 null，其它业务错误继续按失败返回。
     */
    suspend fun verifyReader(cardNo: String): CampusResult<CampusPodReader?> =
        campusQuery(CampusServiceTarget.POD) {
            val connection = connectionOrThrow()
            try {
                client.verifyReader(connection.cookie, cardNo, connection.baseUrl)?.toPublic()
            } catch (failure: SilentPodException) {
                if (failure.kind == SilentPodFailureKind.API && failure.code == "NO_EXISTS") {
                    null
                } else {
                    throw failure
                }
            }
        }

    suspend fun addContact(
        name: String,
        cardNo: String,
        mobile: String?,
    ): CampusResult<CampusPodContact> =
        campusQuery(CampusServiceTarget.POD) {
            val connection = connectionOrThrow()
            client.createContact(
                cookieHeader = connection.cookie,
                name = name,
                cardNo = cardNo,
                mobile = mobile,
                baseUrl = connection.baseUrl,
            ).toPublic()
        }

    suspend fun removeContact(id: Int): CampusResult<Unit> =
        campusQuery(CampusServiceTarget.POD) {
            val connection = connectionOrThrow()
            client.removeContact(connection.cookie, id, connection.baseUrl)
        }

    fun startOptions(space: CampusPodSpace, nowMinutes: Int): List<Int> =
        SilentPodSlotPlanner.startOptions(space.toModel(), nowMinutes)

    fun endOptions(space: CampusPodSpace, startMinutes: Int): List<Int> =
        SilentPodSlotPlanner.endOptions(space.toModel(), startMinutes)

    /** 校园网内直连，校外走 aTrust 代理域；Cookie 挂在哪个域就发到哪个域。 */
    private fun connectionOrThrow(): PodConnection {
        val direct = SilentPodSessionPolicy.sessionCookie(
            cookies.header(SilentPodSessionPolicy.DIRECT_ORIGIN),
        )
        if (direct != null) return PodConnection(direct, SilentPodSessionPolicy.API_BASE_URL)
        val gateway = SilentPodSessionPolicy.sessionCookie(
            cookies.header(SilentPodSessionPolicy.GATEWAY_ORIGIN),
        )
        if (gateway != null) return PodConnection(gateway, SilentPodSessionPolicy.GATEWAY_API_BASE_URL)
        throw SilentPodException(SilentPodFailureKind.AUTHENTICATION)
    }

    private data class PodConnection(val cookie: String, val baseUrl: String)
}
