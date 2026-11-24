package com.stupidtree.hita.plugin.campus

import com.stupidtree.hita.api.CampusCardHistoryRange
import com.stupidtree.hita.api.CampusCardSummary
import com.stupidtree.hita.api.CampusCardTransactions
import com.stupidtree.hita.api.CampusLabProgress
import com.stupidtree.hita.api.CampusLibrarySnapshot
import com.stupidtree.hita.api.CampusPodContact
import com.stupidtree.hita.api.CampusPodContacts
import com.stupidtree.hita.api.CampusPodGroup
import com.stupidtree.hita.api.CampusPodReader
import com.stupidtree.hita.api.CampusPodReservation
import com.stupidtree.hita.api.CampusPodReservationRequest
import com.stupidtree.hita.api.CampusPodSpace
import com.stupidtree.hita.api.CampusResult
import com.stupidtree.hita.api.CampusService
import com.stupidtree.hita.api.CampusSessionState
import com.stupidtree.hita.plugin.campus.source.CampusCardSessionPolicy
import com.stupidtree.hita.plugin.campus.source.LabCenterSessionPolicy
import com.stupidtree.hita.plugin.campus.source.LibrarySmartWallClient
import com.stupidtree.hita.plugin.campus.source.SilentPodSessionPolicy

/**
 * [CampusService] 实现：把四个独立门户聚合为一个服务。
 *
 * 本类不缓存数据、不保存凭据；会话状态每次从浏览器 Cookie 现算。
 */
internal class CampusServiceImpl(
    private val cookies: CampusCookieStore,
    private val card: CampusCardPortal = CampusCardPortal(cookies),
    private val pod: CampusPodPortal = CampusPodPortal(cookies),
    private val library: CampusLibraryPortal = CampusLibraryPortal(cookies),
    private val lab: CampusLabPortal = CampusLabPortal(cookies),
) : CampusService {

    override fun session(): CampusSessionState = CampusSessionState(
        card = cookies.header(CampusCardSessionPolicy.PORTAL_ORIGIN).isNotEmpty() ||
            CampusCardSessionPolicy.hasTicket(cookies.header(CampusCardSessionPolicy.CAS_ORIGIN)),
        pod = SilentPodSessionPolicy.hasSession(
            cookies.header(SilentPodSessionPolicy.DIRECT_ORIGIN),
        ) || SilentPodSessionPolicy.hasSession(
            cookies.header(SilentPodSessionPolicy.GATEWAY_ORIGIN),
        ),
        library = cookies.header(LibrarySmartWallClient.PORTAL_URL).isNotEmpty(),
        lab = LabCenterSessionPolicy.cookieHeader(
            cookies.header(LabCenterSessionPolicy.ORIGIN),
        ) != null,
    )

    override suspend fun cardSummary(): CampusResult<CampusCardSummary> = card.summary()

    override suspend fun cardTransactions(
        page: Int,
        pageSize: Int,
        range: CampusCardHistoryRange,
    ): CampusResult<CampusCardTransactions> = card.transactions(page, pageSize, range)

    override suspend fun podSpaces(date: String?): CampusResult<List<CampusPodGroup>> =
        pod.spaces(date)

    override suspend fun podReservations(): CampusResult<List<CampusPodReservation>> =
        pod.reservations()

    override suspend fun createPodReservation(
        request: CampusPodReservationRequest,
    ): CampusResult<Int?> = pod.create(request)

    override suspend fun cancelPodReservation(
        reservationId: Int,
        reason: String?,
    ): CampusResult<Unit> = pod.cancel(reservationId, reason)

    override suspend fun podContacts(): CampusResult<CampusPodContacts> = pod.contacts()

    override suspend fun verifyPodReader(cardNo: String): CampusResult<CampusPodReader?> =
        pod.verifyReader(cardNo)

    override suspend fun addPodContact(
        name: String,
        cardNo: String,
        mobile: String?,
    ): CampusResult<CampusPodContact> = pod.addContact(name, cardNo, mobile)

    override suspend fun removePodContact(id: Int): CampusResult<Unit> = pod.removeContact(id)

    override suspend fun librarySmartWall(): CampusResult<CampusLibrarySnapshot> =
        library.snapshot()

    override suspend fun labProgress(): CampusResult<CampusLabProgress> = lab.progress()

    override fun podStartOptions(space: CampusPodSpace, nowMinutes: Int): List<Int> =
        pod.startOptions(space, nowMinutes)

    override fun podEndOptions(space: CampusPodSpace, startMinutes: Int): List<Int> =
        pod.endOptions(space, startMinutes)
}
