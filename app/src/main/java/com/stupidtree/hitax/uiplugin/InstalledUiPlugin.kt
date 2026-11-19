package com.stupidtree.hitax.uiplugin

import com.stupidtree.hita.api.MainTabSpec

/** 已安装的运行时界面插件（Compose 容器）。 */
data class InstalledUiPlugin(
    val id: String,
    val name: String,
    val version: String,
    val entryClass: String,
    val apkPath: String,
    val sha256: String,
    val capabilities: List<String> = emptyList(),
    val enabled: Boolean = true,
    /** 主界面 tab 声明；null 表示旧记录，按 capabilities 推断。 */
    val mainTabs: List<MainTabSpec>? = null,
)
