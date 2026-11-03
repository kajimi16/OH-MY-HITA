package com.stupidtree.hita.theta

import android.app.Activity
import android.content.Context
import com.stupidtree.hita.api.CommunityService
import com.stupidtree.hita.theta.utils.ActivityTools

/**
 * [CommunityService] 的 θ 实现。
 *
 * 宿主只看到接口，因此 θ 插件缺席时，资料页/详情页只是少几个入口，不会编译失败、不会崩。
 */
class ThetaCommunityService : CommunityService {

    override fun openUserList(context: Context, title: String, mode: String, extra: String?) {
        ActivityTools.startUserListActivity(context, title, mode, extra)
    }

    override fun openArticleList(context: Context, title: String, mode: String, extra: String?) {
        ActivityTools.startArticleListActivity(context, title, mode, extra)
    }

    override fun openImageGallery(activity: Activity, urls: List<String>, index: Int, absolute: Boolean) {
        ActivityTools.showMultipleImages(activity, urls, index, abs = absolute)
    }
}
