package com.stupidtree.hitax.shell

import androidx.compose.ui.unit.dp

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * HITA-Android shell palette for the seven ThemeTools styles.
 * The XML themes only affect AppCompat; the shell is Compose, so the same
 * style key must produce a Compose color scheme here.
 */
fun shellStyleScheme(style: String, dark: Boolean): ColorScheme {
    val base = if (dark) {
        darkColorScheme(
            primary = Color(0xFF6FB3F2), onPrimary = Color(0xFF001B33),
            primaryContainer = Color(0xFF1E3A5F), onPrimaryContainer = Color(0xFFD6E4F5),
            background = Color(0xFF101317), surface = Color(0xFF1A1E24),
            onSurface = Color(0xFFF2F4F7), onSurfaceVariant = Color(0xFFAEB6C2),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF3390EC), onPrimary = Color.White,
            primaryContainer = Color(0xFFEBF4FC), onPrimaryContainer = Color(0xFF003366),
            background = Color(0xFFF6F7FA), surface = Color.White,
            onSurface = Color.Black, onSurfaceVariant = Color(0xFF6B7280),
        )
    }
    return when (style) {
        "apple_glass" -> base.copy(
            primary = if (dark) Color(0xFF0A6CD6) else Color(0xFF007AFF),
            onPrimary = Color.White,
            primaryContainer = if (dark) Color(0xFF12324E) else Color(0xFFD9ECFF),
            onPrimaryContainer = if (dark) Color(0xFFD6EAFF) else Color(0xFF00315F),
            background = if (dark) Color(0xFF090B10) else Color(0xFFE0EFFC),
            surface = if (dark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.58f),
            onSurface = if (dark) Color(0xFFF5F7FA) else Color(0xFF101318),
            onSurfaceVariant = if (dark) Color(0xFFCCD2DA) else Color(0xFF505761),
            surfaceVariant = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.38f),
        )
        "cyber" -> base.copy(
            primary = if (dark) Color(0xFF22D3EE) else Color(0xFF0891B2),
            onPrimary = if (dark) Color(0xFF03131A) else Color.White,
            primaryContainer = if (dark) Color(0xFF0C4457) else Color(0xFFCFF6FE),
            onPrimaryContainer = if (dark) Color(0xFFB8F3FF) else Color(0xFF083344),
            background = if (dark) Color(0xFF04050C) else Color(0xFFF3F6FC),
            surface = if (dark) Color(0xFF0A0E1D) else Color.White,
            onSurface = if (dark) Color(0xFFE6EDF7) else Color(0xFF10182B),
            onSurfaceVariant = if (dark) Color(0xFF93A0BE) else Color(0xFF4A5570),
        )
        "sora_cloud" -> base.copy(
            primary = if (dark) Color(0xFFF06A52) else Color(0xFFC94B38),
            onPrimary = if (dark) Color(0xFF2A0C08) else Color(0xFFFFF8E8),
            primaryContainer = if (dark) Color(0xFF6F2A21) else Color(0xFFF1C8B8),
            onPrimaryContainer = if (dark) Color(0xFFFFDAD2) else Color(0xFF4C120B),
            background = if (dark) Color(0xFF181714) else Color(0xFFF3EBD9),
            surface = if (dark) Color(0xFF25221D) else Color(0xFFFFF8E8),
            onSurface = if (dark) Color(0xFFF4ECD9) else Color(0xFF25221F),
            onSurfaceVariant = if (dark) Color(0xFFC9BEA8) else Color(0xFF625B50),
        )
        "p5" -> base.copy(
            primary = Color(0xFFE60012), onPrimary = Color.White,
            primaryContainer = Color(0xFFB50020), onPrimaryContainer = Color.White,
            background = if (dark) Color(0xFF0B0B0C) else Color(0xFFF4F4F4),
            surface = if (dark) Color(0xFF17171A) else Color.White,
            onSurface = if (dark) Color(0xFFF5F5F5) else Color(0xFF101010),
            onSurfaceVariant = if (dark) Color(0xFFB9B9C0) else Color(0xFF4A4A4A),
        )
        "deep_space" -> base.copy(
            primary = if (dark) Color(0xFFE3C878) else Color(0xFF8A6D1F),
            onPrimary = if (dark) Color(0xFF221806) else Color(0xFFFFFDF5),
            primaryContainer = if (dark) Color(0xFF473713) else Color(0xFFF0E2B8),
            onPrimaryContainer = if (dark) Color(0xFFF7E4B2) else Color(0xFF3D2E05),
            background = if (dark) Color(0xFF04060D) else Color(0xFFF1EEE7),
            surface = if (dark) Color(0xFF0B1020) else Color(0xFFFDFBF4),
            onSurface = if (dark) Color(0xFFEDEEF0) else Color(0xFF1D2129),
            onSurfaceVariant = if (dark) Color(0xFFA9B0BE) else Color(0xFF565E6E),
        )
        "sumi" -> base.copy(
            primary = if (dark) Color(0xFFE4DCCB) else Color(0xFF33322F),
            onPrimary = if (dark) Color(0xFF1C1A16) else Color(0xFFF7F2E7),
            primaryContainer = if (dark) Color(0xFF3A362E) else Color(0xFFDDD4C2),
            onPrimaryContainer = if (dark) Color(0xFFEDE5D3) else Color(0xFF2A2926),
            background = if (dark) Color(0xFF161512) else Color(0xFFF4EEE1),
            surface = if (dark) Color(0xFF211F1B) else Color(0xFFFBF7EC),
            onSurface = if (dark) Color(0xFFE9E2D2) else Color(0xFF262521),
            onSurfaceVariant = if (dark) Color(0xFFA79D8B) else Color(0xFF6B655A),
        )
        else -> base
    }
}

/** Style-specific font scheme; same font files as upstream HITA_Android. */
fun shellStyleTypography(style: String): androidx.compose.material3.Typography {
    val family = when (style) {
        "apple_glass" -> androidx.compose.ui.text.font.FontFamily(
            androidx.compose.ui.text.font.Font(com.stupidtree.hitax.R.font.inter)
        )
        "cyber" -> androidx.compose.ui.text.font.FontFamily(
            androidx.compose.ui.text.font.Font(com.stupidtree.hitax.R.font.orbitron)
        )
        "sora_cloud" -> androidx.compose.ui.text.font.FontFamily.Serif
        "p5" -> androidx.compose.ui.text.font.FontFamily(
            androidx.compose.ui.text.font.Font(
                com.stupidtree.hitax.R.font.p5_title,
                androidx.compose.ui.text.font.FontWeight.Black,
            )
        )
        "deep_space" -> androidx.compose.ui.text.font.FontFamily(
            androidx.compose.ui.text.font.Font(com.stupidtree.hitax.R.font.space_grotesk)
        )
        "sumi" -> androidx.compose.ui.text.font.FontFamily(
            androidx.compose.ui.text.font.Font(
                com.stupidtree.hitax.R.font.sumi_title,
                androidx.compose.ui.text.font.FontWeight.Bold,
            )
        )
        else -> androidx.compose.ui.text.font.FontFamily.SansSerif
    }
    val base = androidx.compose.material3.Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family),
    )
}

/** Style-specific Material3 shapes; Sora uses cut corners, P5/Cyber are sharp. */
fun shellStyleShapes(style: String): androidx.compose.material3.Shapes {
    val small = when (style) {
        "apple_glass" -> 18.dp
        "cyber" -> 6.dp
        "sora_cloud" -> 14.dp
        "p5" -> 3.dp
        "deep_space" -> 16.dp
        "sumi" -> 8.dp
        else -> 8.dp
    }
    val medium = when (style) {
        "apple_glass" -> 26.dp
        "cyber" -> 10.dp
        "sora_cloud" -> 18.dp
        "p5" -> 4.dp
        "deep_space" -> 22.dp
        "sumi" -> 12.dp
        else -> 12.dp
    }
    val large = when (style) {
        "apple_glass" -> 32.dp
        "cyber" -> 14.dp
        "sora_cloud" -> 22.dp
        "p5" -> 6.dp
        "deep_space" -> 28.dp
        "sumi" -> 16.dp
        else -> 16.dp
    }
    val shape = { radius: androidx.compose.ui.unit.Dp ->
        if (style == "sora_cloud") {
            androidx.compose.foundation.shape.CutCornerShape(
                topEnd = radius,
                bottomStart = radius / 3,
            )
        } else {
            androidx.compose.foundation.shape.RoundedCornerShape(radius)
        }
    }
    return androidx.compose.material3.Shapes(
        extraSmall = shape(small),
        small = shape(small),
        medium = shape(medium),
        large = shape(large),
        extraLarge = shape(large),
    )
}
