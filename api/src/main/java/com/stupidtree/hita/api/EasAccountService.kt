package com.stupidtree.hita.api

import com.stupidtree.hita.api.dto.EasAccountDto

/**
 * 教务账号快照（API 14）。
 *
 * 纯数据契约：不包含页面跳转、Activity、token、cookie 或 password。
 * 由 `plugin-eas` 提供；插件缺失时宿主取不到服务。
 */
interface EasAccountService {
    /**
     * 任意线程可调用。返回当前账号的不可变快照；未登录时 `loggedIn=false`，
     * 业务字段全部为 `null`。
     */
    fun account(): EasAccountDto

    companion object {
        val KEY = ServiceKey<EasAccountService>("hita.eas.account")
    }
}
