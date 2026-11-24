package com.stupidtree.hita.plugin.campusui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stupidtree.hita.api.PluginUiHost

/** 宿主没有提供 CampusService 时显示，仍允许关闭。 */
@Composable
internal fun ServiceUnavailable(host: PluginUiHost) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("校园服务不可用", style = MaterialTheme.typography.titleMedium)
        Text(
            "宿主没有注册 CampusService（hita.campus），请确认校园扩展插件已启用。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { host.close() }, modifier = Modifier.padding(top = 8.dp)) {
            Text("关闭")
        }
    }
}

/** 查询中的占位。 */
@Composable
internal fun LoadingBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(strokeWidth = 2.dp)
    }
}

/** 失败占位；会话缺失时额外给登录入口。 */
@Composable
internal fun FailureCard(
    state: LoadState.Failed,
    onRetry: () -> Unit,
    onLogin: (() -> Unit)?,
    modifier: Modifier = Modifier,
    loginButtonFullWidth: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(state.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onLogin != null) {
            Button(
                onClick = onLogin,
                modifier = if (loginButtonFullWidth) {
                    Modifier.fillMaxWidth().padding(top = 16.dp)
                } else {
                    Modifier.padding(top = 16.dp).width(220.dp)
                },
            ) { Text("去登录") }
        } else {
            TextButton(onClick = onRetry) { Text("重试") }
        }
    }
}

/** 列表分组标题。 */
@Composable
internal fun SectionHeader(
    title: String,
    count: Int? = null,
    expanded: Boolean? = null,
    onToggle: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onToggle != null) {
            Text(
                if (expanded == true) "▾" else "▸",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onToggle() }
                    .padding(end = 8.dp),
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f).then(
                if (onToggle != null) Modifier.clickable { onToggle() } else Modifier
            ),
        )
        if (count != null) {
            Text(
                "$count",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** 一行标签与值的展示。 */
@Composable
internal fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
