package com.stupidtree.hitax.market

/**
 * 插件详情主按钮的版本动作。
 *
 * 选择非最新版本时必须是安装/降级动作，不能因为“已安装”就把按钮吞掉。
 */
object MarketVersionAction {

    enum class Kind { INSTALL, UPDATE, NONE }

    fun resolve(
        installed: Boolean,
        selectedVersion: String,
        latestVersion: String?,
        updateAvailable: Boolean,
    ): Kind {
        if (!installed) return Kind.INSTALL
        if (latestVersion == null || PluginVersion.compare(selectedVersion, latestVersion) != 0) {
            return Kind.INSTALL
        }
        return if (updateAvailable) Kind.UPDATE else Kind.NONE
    }
}
