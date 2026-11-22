package com.stupidtree.hitax.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * First-run value proposition for OH-MY-HITA.
 * Caller supplies system-bar padding; this screen consumes none.
 */
@Composable
fun OnboardingIntroScreen(
    onContinue: () -> Unit,
    onStartOver: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
    ) {
        BrandRow()
        Spacer(Modifier.height(28.dp))
        HeroGraphic()
        Spacer(Modifier.height(32.dp))
        Text(
            text = "万物皆插件",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "OH-MY-HITA 由微内核和插件组成。每一页、每项服务、每个工具都能被替换，整合包帮你一次搭好顺手的组合。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        FeatureRow(Icons.Rounded.Memory, "微内核", "核心只保留调度与接口，其余能力按需装载。")
        Spacer(Modifier.height(18.dp))
        FeatureRow(Icons.Rounded.Tune, "按需组合", "用整合包一键装配页面、服务与工具。")
        Spacer(Modifier.height(18.dp))
        FeatureRow(Icons.Rounded.SwapHoriz, "随时切换", "换一个整合包就是换一种用法，不改数据。")
        Spacer(Modifier.height(36.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("选择整合包", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onStartOver,
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("从零开始", style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "之后可在插件市场切换",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BrandRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "H",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(
            text = "OH-MY-HITA",
            modifier = Modifier.padding(start = 10.dp).weight(1f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text(
                text = "首次启动 · 1 分钟",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun HeroGraphic() {
    Box(
        modifier = Modifier.fillMaxWidth().height(216.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
        )
        PluginTile(Icons.Rounded.AccessTime, Modifier.align(Alignment.TopStart).offset(x = 20.dp, y = 26.dp))
        PluginTile(Icons.Rounded.Article, Modifier.align(Alignment.TopEnd).offset(x = -18.dp, y = 4.dp))
        PluginTile(Icons.Rounded.Insights, Modifier.align(Alignment.BottomStart).offset(x = 8.dp, y = -22.dp))
        PluginTile(Icons.Rounded.Extension, Modifier.align(Alignment.BottomEnd).offset(x = -14.dp, y = -18.dp))
        Surface(
            modifier = Modifier.size(118.dp),
            shape = RoundedCornerShape(34.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 10.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Memory, contentDescription = null, modifier = Modifier.size(34.dp))
                    Text(
                        text = "微内核",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PluginTile(icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(46.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(21.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                modifier = Modifier.padding(top = 3.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
