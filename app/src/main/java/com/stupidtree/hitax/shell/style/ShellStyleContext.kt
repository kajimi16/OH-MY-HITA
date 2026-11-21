package com.stupidtree.hitax.shell.style

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.stupidtree.style.ThemeTools

/** 当前 shell 风格；由 PresetShellActivity 在 MaterialTheme 外层提供。 */
data class ShellStyleState(val key: String, val dark: Boolean)

val LocalShellStyle = staticCompositionLocalOf { ShellStyleState("classic", false) }

object ShellStyle {
    val key: String
        @Composable @ReadOnlyComposable get() = LocalShellStyle.current.key
    val dark: Boolean
        @Composable @ReadOnlyComposable get() = LocalShellStyle.current.dark
}

/**
 * 上游 HitaTheme 的最小适配层。
 *
 * 上游 design 包中的 Surface 文件只读 `preferenceStyle` 与 `isDark`；
 * 这里把宿主 ThemeController 的字符串风格映射回上游枚举，保持移植文件主体不变。
 */
object HitaTheme {
    val preferenceStyle: ThemeTools.STYLE
        @Composable @ReadOnlyComposable
        get() = when (ShellStyle.key) {
            "apple_glass" -> ThemeTools.STYLE.APPLE_GLASS
            "cyber" -> ThemeTools.STYLE.CYBER
            "sora_cloud" -> ThemeTools.STYLE.SORA_CLOUD
            "p5" -> ThemeTools.STYLE.P5
            "deep_space" -> ThemeTools.STYLE.DEEP_SPACE
            "sumi" -> ThemeTools.STYLE.SUMI
            else -> ThemeTools.STYLE.CLASSIC
        }

    val isDark: Boolean
        @Composable @ReadOnlyComposable get() = ShellStyle.dark
}
