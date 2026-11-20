package com.stupidtree.hita.api

import androidx.lifecycle.LiveData
import com.stupidtree.component.data.DataState

/**
 * Headless EAS login contract (API 11). T2 supplies credentials; T1 owns the
 * token and network session. UI Activities never enter this contract.
 */
interface EasAuthService {
    fun isLogin(): Boolean
    fun accountName(): String?
    fun accountSubtitle(): String?

    fun login(username: String, password: String): LiveData<DataState<Boolean>>
    fun logout()

    companion object {
        val KEY = ServiceKey<EasAuthService>("hita.eas.auth")
    }
}
