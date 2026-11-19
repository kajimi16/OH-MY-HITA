package com.stupidtree.hita.api

import androidx.fragment.app.Fragment

/**
 * 主界面 tab 贡献。
 *
 * 宿主主界面按 id 从注册表取 Fragment；插件缺席时该槽位显示空页面，
 * 宿主不再 import 具体 Fragment 类。
 */
class TabContribution(
    val id: String,
    val titleRes: Int,
    val iconRes: Int,
    val order: Int,
    val create: () -> Fragment,
)

interface TabRegistry {
    fun register(tab: TabContribution)
    fun entries(): List<TabContribution>
    fun find(id: String): TabContribution?
}
