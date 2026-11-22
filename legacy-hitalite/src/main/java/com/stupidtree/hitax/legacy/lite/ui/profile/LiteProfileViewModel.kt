package com.stupidtree.hitax.legacy.lite.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.stupidtree.hita.api.UserProfileSnapshot
import com.stupidtree.hitax.legacy.lite.LiteHostBridge

/**
 * HITA_Lite 资料页的只读状态。
 *
 * 前置条件：`LiteHostBridge.userProfile` 已注入；缺失时暴露空快照。
 * 后置条件：页面只读取当前会话快照，不创建本地用户库或网络请求。
 */
class LiteProfileViewModel : ViewModel() {

    private val service = LiteHostBridge.userProfile

    val profileLiveData: LiveData<UserProfileSnapshot?> =
        service?.current() ?: MutableLiveData(null)

    fun logout() {
        service?.logout()
    }
}
