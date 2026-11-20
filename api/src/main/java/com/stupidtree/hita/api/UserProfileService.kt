package com.stupidtree.hita.api

import androidx.lifecycle.LiveData

/** 当前登录用户的只读快照；token 不进入 UI 契约。 */
data class UserProfileSnapshot(
    val id: String,
    val username: String?,
    val nickname: String?,
    val signature: String?,
    val avatar: String?,
    val school: String?,
    val studentId: String?,
)

/**
 * 本地账号会话（API 11）。
 *
 * T1 持有 SharedPreferences/用户库；T2 只读取快照并触发登出。
 */
interface UserProfileService {
    fun current(): LiveData<UserProfileSnapshot?>

    fun logout()

    companion object {
        val KEY = ServiceKey<UserProfileService>("hita.user.profile")
    }
}
