package com.stupidtree.hitax.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingPresetScreen(
    state: OnboardingUiState,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onApply: () -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
) {
    BackHandler(enabled = !state.applying, onBack = onBack)
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(onBack)
            Box(Modifier.fillMaxWidth().weight(1f)) {
                when {
                    state.loading -> LoadingBody()
                    state.error != null -> ErrorBody(state.error, onRetry)
                    state.presets.isEmpty() -> EmptyBody(onRetry)
                    else -> PresetList(state, onSelect)
                }
            }
            BottomBar(state, onApply, onSkip)
        }
        if (state.applying) ApplyingOverlay(state.applyMessage)
    }
}

@Composable
private fun ScreenHeader(onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "2 / 2",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "选择你的起点",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "整合包只是一个插件组合，之后随时可换",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PresetList(state: OnboardingUiState, onSelect: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.presets, key = { it.id }) { preset ->
            PresetCard(
                preset = preset,
                selected = preset.id == state.selectedId,
                enabled = !state.applying,
                onClick = { onSelect(preset.id) },
            )
        }
    }
}

@Composable
private fun PresetCard(
    preset: OnboardingPresetUi,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) scheme.primary else scheme.outlineVariant,
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) scheme.primaryContainer.copy(alpha = 0.22f) else scheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonogramTile(preset.id, preset.name)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            preset.name,
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = scheme.onSurface,
                        )
                        if (preset.recommended) {
                            Spacer(Modifier.width(6.dp))
                            PresetBadge("推荐", scheme.primaryContainer, scheme.onPrimaryContainer)
                        }
                        if (preset.active) {
                            Spacer(Modifier.width(6.dp))
                            PresetBadge("使用中", scheme.secondaryContainer, scheme.onSecondaryContainer)
                        }
                    }
                    val source = preset.source?.trim().orEmpty()
                    if (source.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "来自 $source",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (selected) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.Check, contentDescription = "已选择", tint = scheme.primary)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                preset.description,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "${preset.pluginCount} 个插件",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PresetBadge(
    text: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
) {
    Surface(shape = RoundedCornerShape(8.dp), color = container) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = content,
        )
    }
}

@Composable
private fun MonogramTile(id: String, name: String) {
    val scheme = MaterialTheme.colorScheme
    val palette = remember(scheme) {
        listOf(
            scheme.primaryContainer to scheme.onPrimaryContainer,
            scheme.secondaryContainer to scheme.onSecondaryContainer,
            scheme.tertiaryContainer to scheme.onTertiaryContainer,
            scheme.surfaceVariant to scheme.onSurfaceVariant,
        )
    }
    val index = ((id.hashCode() % palette.size) + palette.size) % palette.size
    val (background, foreground) = palette[index]
    val monogram = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Box(
        Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            monogram,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = foreground,
        )
    }
}

@Composable
private fun BottomBar(state: OnboardingUiState, onApply: () -> Unit, onSkip: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onApply,
            enabled = state.selectedId != null && !state.applying && !state.loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("应用并开始", style = MaterialTheme.typography.titleMedium)
        }
        TextButton(
            onClick = onSkip,
            enabled = !state.applying,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("先不选择", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoadingBody() {
    val transition = rememberInfiniteTransition(label = "presetSkeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "presetSkeletonAlpha",
    )
    Column(Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 4.dp)) {
        repeat(4) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).alpha(pulse),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonBar(0.5f)
                        SkeletonBar(0.8f)
                        SkeletonBar(1f)
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                "正在读取整合包…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SkeletonBar(widthFraction: Float) {
    Box(
        Modifier.fillMaxWidth(widthFraction).height(12.dp).clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
private fun ErrorBody(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("加载失败", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry, shape = RoundedCornerShape(16.dp)) { Text("重试") }
    }
}

@Composable
private fun EmptyBody(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("暂无可用的整合包", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "你可以先跳过，稍后在设置里再选择",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRetry) { Text("重新加载") }
    }
}

@Composable
private fun ApplyingOverlay(message: String?) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
            .clickable(interactionSource = interaction, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                Modifier.padding(horizontal = 32.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(Modifier.size(36.dp), strokeWidth = 3.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    message ?: "正在应用…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
