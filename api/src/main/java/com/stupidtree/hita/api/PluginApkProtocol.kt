package com.stupidtree.hita.api

/**
 * 独立 APK 插件协议（T2）。
 *
 * 插件是一个独立应用，自带界面与资源；宿主通过这个 intent action 发现它，
 * 通过 `<meta-data>` 读取契约版本，通过启动 Activity 打开它的界面。
 *
 * 与 T1（dex 插件）的区别：T2 有完整清单、资源和独立进程边界，
 * 因此可以拥有界面；代价是必须单独安装、单独更新。
 */
object PluginApkProtocol {

    /** 插件入口 Activity 上声明的 action。 */
    const val ACTION = "com.stupidtree.hita.action.PLUGIN_APK"

    /** `<application>` 下的 meta-data：插件依赖的契约版本。 */
    const val META_API_VERSION = "hita.plugin.apiVersion"

    /** `<application>` 下的 meta-data：插件唯一 id（建议反向域名）。 */
    const val META_ID = "hita.plugin.id"
}
