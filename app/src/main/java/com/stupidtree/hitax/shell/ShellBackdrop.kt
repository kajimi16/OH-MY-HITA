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
                if (dark) {
                    drawRect(Brush.verticalGradient(listOf(Color(0xFF04050C), Color(0xFF0A0E1D), Color(0xFF04050C))))
                } else {
                    drawRect(Brush.verticalGradient(listOf(Color(0xFFF3F6FC), Color(0xFFE6F7FB), Color(0xFFF3F6FC))))
                }
                val step = size.width / 12f
                for (i in 0..12) {
                    drawLine((if (dark) Color(0xFF22D3EE) else Color(0xFF0891B2)).copy(alpha = if (dark) 0.10f else 0.08f), Offset(i * step, 0f), Offset(i * step, size.height), 1f)
                }
                val hStep = size.height / 18f
                for (i in 0..18) {
                    drawLine((if (dark) Color(0xFFA78BFA) else Color(0xFF7C5CD6)).copy(alpha = if (dark) 0.08f else 0.06f), Offset(0f, i * hStep), Offset(size.width, i * hStep), 1f)
                }
            }
            "sora_cloud" -> Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.verticalGradient(listOf(Color(0xFFF3EBD9), Color(0xFFFFF8E8))))
                drawCircle(Color(0xFFD2A23C).copy(alpha = 0.25f), radius = size.minDimension * 0.2f, center = Offset(size.width * 0.78f, size.height * 0.14f))
                val p = Path().apply {
                    moveTo(0f, size.height * 0.68f)
                    cubicTo(size.width * 0.25f, size.height * 0.58f, size.width * 0.5f, size.height * 0.80f, size.width, size.height * 0.66f)
                }
                drawPath(p, Color(0xFF315D6D).copy(alpha = 0.12f), style = Stroke(width = 3f))
            }
            "p5" -> Canvas(Modifier.fillMaxSize()) {
                drawRect(Color(0xFFF4F4F4))
                val stripe = size.width / 9f
                var x = -size.height
                while (x < size.width) {
                    val p = Path().apply {
                        moveTo(x, 0f); lineTo(x + stripe, 0f); lineTo(x + stripe + size.height, size.height); lineTo(x + size.height, size.height); close()
                    }
                    drawPath(p, Color(0xFFE60012).copy(alpha = 0.07f))
                    x += stripe * 2
                }
                drawRect(Color(0xFF101010).copy(alpha = 0.04f), size = Size(size.width, size.height * 0.06f))
            }
            "deep_space" -> Canvas(Modifier.fillMaxSize()) {
                if (dark) {
                    drawRect(Brush.verticalGradient(listOf(Color(0xFF04060D), Color(0xFF0B1020), Color(0xFF04060D))))
                } else {
                    drawRect(Brush.verticalGradient(listOf(Color(0xFFF1EEE7), Color(0xFFFDFBF4), Color(0xFFF1EEE7))))
                }
                stars.forEachIndexed { i, (x, y) ->
                    drawCircle(
                        (if (dark) Color.White else Color(0xFF8A6D1F)).copy(alpha = 0.18f + (i % 5) * 0.06f),
                        radius = 1.2f + (i % 3),
                        center = Offset(x * size.width, y * size.height),
                    )
                }
                drawCircle(Color(0xFFE3C878).copy(alpha = 0.14f), radius = size.minDimension * 0.22f, center = Offset(size.width * 0.82f, size.height * 0.16f))
            }
            "sumi" -> Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.verticalGradient(listOf(Color(0xFFF4EEE1), Color(0xFFFBF7EC))))
                val p = Path().apply {
                    moveTo(0f, size.height * 0.78f)
                    cubicTo(size.width * 0.22f, size.height * 0.70f, size.width * 0.42f, size.height * 0.86f, size.width, size.height * 0.74f)
                }
                drawPath(p, Color(0xFF33322F).copy(alpha = 0.10f), style = Stroke(width = 10f))
                drawLine(Color(0xFFA63B2A).copy(alpha = 0.25f), Offset(size.width * 0.86f, size.height * 0.10f), Offset(size.width * 0.86f, size.height * 0.22f), 2f)
            }
            else -> Unit
        }
    }
}
