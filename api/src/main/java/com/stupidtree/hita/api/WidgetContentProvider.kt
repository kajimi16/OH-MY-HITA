package com.stupidtree.hita.api

import android.content.Context

/** 小组件尺寸，单位 dp；宿主可能按实际摆放换算。 */
data class WidgetOptions(
    val widthDp: Int,
    val heightDp: Int,
)

/** 一行小组件内容；宿主负责统一排版与点击。 */
data class WidgetRow(
    val leading: String? = null,
    val title: String,
    val trailing: String? = null,
)

/** 插件返回给宿主的纯数据快照，不包含 RemoteViews 或资源 ID。 */
data class WidgetContent(
    val title: String,
    val subtitle: String? = null,
    val rows: List<WidgetRow> = emptyList(),
)

/** 小组件渲染时可用的宿主服务。 */
interface WidgetHost {
    fun <T : Any> service(key: ServiceKey<T>): T?
}

/**
 * 市场 UI 插件可实现此接口，宿主会把内容渲染到统一的桌面小组件里。
 *
 * 插件没有 manifest，不能自己注册 AppWidgetProvider；因此小组件由宿主提供，
 * 插件只负责给数据。
 */
interface WidgetContentProvider {
    fun buildWidget(context: Context, host: WidgetHost, options: WidgetOptions): WidgetContent
}
