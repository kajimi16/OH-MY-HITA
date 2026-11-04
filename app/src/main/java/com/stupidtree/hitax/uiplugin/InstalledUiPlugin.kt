package com.stupidtree.hitax.uiplugin

/** 已安装的运行时界面插件（Compose 容器）。 */
data class InstalledUiPlugin(
    val id: String,
    val name: String,
    val version: String,
    val entryClass: String,
    val apkPath: String,
    val sha256: String,
    val capabilities: List<String> = emptyList(),
)
