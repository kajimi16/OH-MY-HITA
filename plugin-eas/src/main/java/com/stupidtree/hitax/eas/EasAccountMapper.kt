package com.stupidtree.hitax.eas

import com.stupidtree.hita.api.dto.EasAccountDto
import com.stupidtree.hitax.data.model.eas.EASToken

/**
 * `EASToken` → 账号 DTO。
 *
 * 约束：
 * - 绝不复制 accessToken / refreshToken / password / cookies / webCookies；
 * - 未登录时业务字段全部为 null；
 * - `subtitle` 用 null 表示未知，不再用空串表示未登录；
 * - `hasApiToken` 只认 bearer token，WebView cookie 会话不算。
 */
internal fun EASToken.toAccountDto(): EasAccountDto {
    val loggedIn = isLogin()
    if (!loggedIn) return EasAccountDto(schemaVersion = 1, loggedIn = false, hasApiToken = false)
    return EasAccountDto(
        schemaVersion = 1,
        loggedIn = true,
        hasApiToken = !accessToken.isNullOrBlank(),
        displayName = name?.ifBlank { username } ?: username,
        subtitle = stuId?.ifBlank { username },
        studentId = stuId,
        avatarUrl = picture,
        studentType = stutype.name,
        school = school,
        major = major,
        grade = grade,
    )
}
