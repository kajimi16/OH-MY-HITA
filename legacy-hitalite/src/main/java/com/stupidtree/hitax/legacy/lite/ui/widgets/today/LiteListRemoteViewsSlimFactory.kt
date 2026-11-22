package com.stupidtree.hitax.legacy.lite.ui.widgets.today

import android.content.Context
import android.content.Intent

/**
 * 窄尺寸列表工厂。
 *
 * 前置条件：`LiteListWidgetService` 收到 `slim=true` 的请求。
 * 后置条件：复用标准工厂的数据读取与主题逻辑，仅切换 `lite_widget_today_item_slim`。
 * 不变式：不新增数据源，不触碰旧数据库。
 */
internal class LiteListRemoteViewsSlimFactory(
    context: Context,
    intent: Intent
) : LiteListRemoteViewsFactory(context, intent, slim = true)
