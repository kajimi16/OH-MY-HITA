package com.stupidtree.hitax.plugin

import android.content.Context

/**
 * 用户卸载（停用）内置插件的持久化状态。
 *
 * 独立于业务偏好：清除应用数据会一并丢失，插件随之恢复——与「重装应用」语义一致。
 */
object PluginPrefs {
    private const val SP = "plugin_state"
    private const val KEY_DISABLED = "disabled_ids"

    fun disabled(context: Context): Set<String> =
        context.getSharedPreferences(SP, Context.MODE_PRIVATE)
            .getStringSet(KEY_DISABLED, emptySet())?.toSet() ?: emptySet()

    fun isDisabled(context: Context, pluginId: String): Boolean =
        pluginId in disabled(context)

    fun setDisabled(context: Context, pluginId: String, disabled: Boolean) {
        val prefs = context.getSharedPreferences(SP, Context.MODE_PRIVATE)
        val next = disabled(context).toMutableSet()
        if (disabled) next.add(pluginId) else next.remove(pluginId)
        prefs.edit().putStringSet(KEY_DISABLED, next).apply()
    }
}
