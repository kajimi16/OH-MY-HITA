package com.stupidtree.hita.api

import android.content.Context
import android.view.View

/**
 * 个人主页插件的界面入口。
 *
 * 宿主只传 userId 与可选的共享元素 View，具体页面由插件实现。
 */
interface UserUiService {
    /** [avatar] 用于头像共享元素动画，可为空。 */
    fun openProfile(context: Context, userId: String?, avatar: View? = null)

    companion object {
        val KEY = ServiceKey<UserUiService>("hita.user.ui")
    }
}
