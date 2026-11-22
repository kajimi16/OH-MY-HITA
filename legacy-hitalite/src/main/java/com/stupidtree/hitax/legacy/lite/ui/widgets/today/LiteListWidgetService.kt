package com.stupidtree.hitax.legacy.lite.ui.widgets.today

import android.content.Intent
import android.widget.RemoteViewsService

/**
 * 今日组件集合视图服务。
 *
 * 前置条件：系统按 RemoteViews 适配器 Intent 绑定本服务。
 * 后置条件：`slim` 为真时返回窄尺寸工厂，否则返回标准工厂。
 * 不变式：不持有数据源，不创建 Room/Repository。
 */
class LiteListWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val slim = intent.getBooleanExtra(LiteTodayUtils.EXTRA_SLIM, false)
        return if (slim) LiteListRemoteViewsSlimFactory(this, intent)
        else LiteListRemoteViewsFactory(this, intent)
    }
}
