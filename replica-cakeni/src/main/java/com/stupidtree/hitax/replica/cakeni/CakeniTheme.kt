package com.stupidtree.hitax.replica.cakeni

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Light = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E8FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF4F6F8F),
    secondaryContainer = Color(0xFFDCE9F7),
    background = Color(0xFFECF3FA),
    surface = Color(0xFFF9FCFF),
    surfaceVariant = Color(0xFFE2EBF4),
    onSurfaceVariant = Color(0xFF52616F),
    outlineVariant = Color(0xFFD2DEEA),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9CCBFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF124A78),
    onPrimaryContainer = Color(0xFFD6E8FF),
    secondary = Color(0xFFB7CBE0),
    secondaryContainer = Color(0xFF34495E),
    background = Color(0xFF101820),
    surface = Color(0xFF18232E),
    surfaceVariant = Color(0xFF263442),
    onSurfaceVariant = Color(0xFFB9C8D6),
    outlineVariant = Color(0xFF3A4A59),
)

@Composable
fun CakeniReplicaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) Dark else Light,
        content = content,
    )
}

object CakeniDimens {
    val page = 18.dp
    val cardRadius = 16.dp
    val courseRadius = 12.dp
    val gap = 10.dp
    val dayWidth = 84.dp
}

object CakeniCourseColors {
    private val palette = listOf(
        0xFFE98484, 0xFF9B8AD6, 0xFF6FB6B0, 0xFF6FA8D9,
        0xFFD67FA6, 0xFF6FBF9A, 0xFFD9A05B,
    ).map { Color(it) }

    fun colorFor(id: String): Color = palette[(id.hashCode() and Int.MAX_VALUE) % palette.size]
}
