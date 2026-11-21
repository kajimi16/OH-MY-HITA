package com.stupidtree.hitax.shell

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.MaterialTheme
import com.stupidtree.hitax.shell.style.hitaSoraCloudBackground
import com.stupidtree.hitax.shell.style.hitaDeepSpaceBackground
import com.stupidtree.hitax.shell.style.hitaSumiBackground
import kotlin.math.sin

/**
 * Style-specific shell backdrop. These are deliberately lightweight Compose
 * surfaces rather than copies of the XML drawables: the shell runs in a
 * runtime Compose container and cannot rely on the appearance plugin's resources.
 */
@Composable
fun ShellBackdrop(style: String, dark: Boolean) {
    val stars = remember(style) {
        List(42) { i ->
            val x = ((i * 73) % 1000) / 1000f
            val y = ((i * 137) % 1000) / 1000f
            x to y
        }
    }
    val cyberBackground = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxSize()) {
        when (style) {
            "apple_glass" -> Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    Brush.verticalGradient(
                        if (dark) listOf(Color(0xFF080B10), Color(0xFF111823), Color(0xFF0C1017))
                        else listOf(Color(0xFFF5F9FE), Color(0xFFEAF3FC), Color(0xFFE0EFFC))
                    )
                )
                // Matches upstream AppleGlassBackground atmosphere overlay.
                drawRect(
                    Brush.verticalGradient(
                        if (dark) listOf(
                            Color.White.copy(alpha = 0.42f),
                            Color.Transparent,
                            Color(0xFF6FB3F2).copy(alpha = 0.12f),
                        ) else listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color(0xFF3390EC).copy(alpha = 0.045f),
                            Color(0xFF9CCBFA).copy(alpha = 0.16f),
                        )
                    )
                )
                // Upstream glass keeps a flat tinted canvas; no decorative blobs.
            }
            "cyber" -> Canvas(Modifier.fillMaxSize()) {
                // 上游 cyber 不绘制装饰背景，氛围由切角/霓虹卡片提供。
                drawRect(cyberBackground)
            }
            "sora_cloud" -> Box(Modifier.fillMaxSize().hitaSoraCloudBackground())
            "p5" -> Canvas(Modifier.fillMaxSize()) {
                // 上游 P5 不绘制装饰背景，材质由错位硬阴影卡片提供。
                drawRect(cyberBackground)
            }
            "deep_space" -> Box(Modifier.fillMaxSize().hitaDeepSpaceBackground())
            "sumi" -> Box(Modifier.fillMaxSize().hitaSumiBackground())
            else -> Unit
        }
    }
}
