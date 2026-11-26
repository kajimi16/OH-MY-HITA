package com.stupidtree.hita.api

import android.content.Context

/**
 * 可选本地化扩展。
 *
 * 旧 [ComposeUiPlugin] 无需实现；宿主只有在 `plugin is LocalizedComposeUiPlugin`
 * 时才调用，避免直接调用 DEX 中已退化为 abstract 的 `ComposeUiPlugin.localizedDisplayName`。
 */
interface LocalizedComposeUiPlugin {
    fun localizedDisplayName(context: Context): String
}
