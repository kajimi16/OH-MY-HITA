package com.stupidtree.hita.api

import android.content.Context
import android.content.Intent

/**
 * 一个导航贡献项。`titleRes`/`iconRes` 来自插件自己的 `R`。
 *
 * 注意：这只在**构建期插件**（同一 APK、资源已合并）下成立；
 * 运行时 DEX 插件拿不到合并后的资源表，届时只能贡献纯代码入口。
 */
class NavEntry(
    val id: String,
    val titleRes: Int,
    val iconRes: Int,
    val order: Int = 100,
    /**
     * 返回 null 表示本次不跳转（插件已自行提示，例如未登录）。
     */
    val createIntent: (Context) -> Intent?,
)

interface NavRegistry {
    fun register(entry: NavEntry)

    /** 按 [NavEntry.order] 排序。 */
    fun entries(): List<NavEntry>

    fun find(id: String): NavEntry?
}
