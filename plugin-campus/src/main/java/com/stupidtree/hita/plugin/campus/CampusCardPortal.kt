package com.stupidtree.hita.plugin.campus

import com.stupidtree.hita.api.CampusCardHistoryRange
import com.stupidtree.hita.api.CampusCardSummary
import com.stupidtree.hita.api.CampusCardTransactions
import com.stupidtree.hita.api.CampusResult
import com.stupidtree.hita.api.CampusServiceTarget
import com.stupidtree.hita.plugin.campus.source.CampusCardClient
import com.stupidtree.hita.plugin.campus.source.CampusCardHistoryRange as InternalHistoryRange
import com.stupidtree.hita.plugin.campus.source.CampusCardSessionPolicy
import com.stupidtree.hita.plugin.campus.source.CampusCardSignInRequiredException

/** 校园卡门户：登录换 token、余额汇总与消费流水。 */
internal class CampusCardPortal(
    private val cookies: CampusCookieStore,
    private val client: CampusCardClient = CampusCardClient(),
) {

    suspend fun summary(): CampusResult<CampusCardSummary> =
        campusQuery(CampusServiceTarget.CARD) {
            val token = tokenOrThrow()
            val balance = client.fetchBalance(token)
            val spending = try {
                client.fetchSpending(token)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            balance.toPublic(spending)
        }

    suspend fun transactions(
        page: Int,
        pageSize: Int,
        range: CampusCardHistoryRange,
    ): CampusResult<CampusCardTransactions> = campusQuery(CampusServiceTarget.CARD) {
        val token = tokenOrThrow()
        client.fetchTransactions(
            token = token,
            page = page,
            pageSize = pageSize,
            range = range.toInternal(),
        ).toPublic()
    }

    private suspend fun tokenOrThrow(): String {
        val portalCookie = cookies.header(CampusCardSessionPolicy.PORTAL_ORIGIN)
        val casCookie = cookies.header(CampusCardSessionPolicy.CAS_ORIGIN)
        if (portalCookie.isEmpty() && casCookie.isEmpty()) {
            throw CampusCardSignInRequiredException("门户登录已失效，请重新登录")
        }
        return client.acquireToken(
            cookieHeader = { origin ->
                when (origin) {
                    CampusCardSessionPolicy.PORTAL_ORIGIN -> portalCookie
                    CampusCardSessionPolicy.CAS_ORIGIN -> casCookie
                    else -> ""
                }
            },
        )
    }

    private fun CampusCardHistoryRange.toInternal(): InternalHistoryRange = when (this) {
        CampusCardHistoryRange.MONTH -> InternalHistoryRange.MONTH
        CampusCardHistoryRange.YEAR -> InternalHistoryRange.YEAR
        CampusCardHistoryRange.ALL -> InternalHistoryRange.ALL
    }
}
