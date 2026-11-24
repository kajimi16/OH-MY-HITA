package com.stupidtree.hita.plugin.campus

import com.stupidtree.hita.api.CampusLabProgress
import com.stupidtree.hita.api.CampusLibrarySnapshot
import com.stupidtree.hita.api.CampusResult
import com.stupidtree.hita.api.CampusServiceTarget
import com.stupidtree.hita.plugin.campus.source.LabCenterClient
import com.stupidtree.hita.plugin.campus.source.LabCenterSessionExpiredException
import com.stupidtree.hita.plugin.campus.source.LabCenterSessionPolicy
import com.stupidtree.hita.plugin.campus.source.LibrarySmartWallClient

/** 图书馆智慧墙：匿名可读，但网关拦截时要求登录。 */
internal class CampusLibraryPortal(
    private val cookies: CampusCookieStore,
    private val client: LibrarySmartWallClient = LibrarySmartWallClient(),
) {
    suspend fun snapshot(): CampusResult<CampusLibrarySnapshot> =
        campusQuery(CampusServiceTarget.LIBRARY) {
            val cookie = cookies.header(LibrarySmartWallClient.PORTAL_URL).ifBlank { null }
            client.fetch(cookie).toPublic()
        }
}

/** 实验中心：只读整学期实验课表。 */
internal class CampusLabPortal(
    private val cookies: CampusCookieStore,
    private val client: LabCenterClient = LabCenterClient(),
) {
    suspend fun progress(): CampusResult<CampusLabProgress> =
        campusQuery(CampusServiceTarget.LAB) {
            val cookie = LabCenterSessionPolicy.cookieHeader(
                cookies.header(LabCenterSessionPolicy.ORIGIN),
            ) ?: throw LabCenterSessionExpiredException()
            client.fetch(cookie).toPublic()
        }
}
