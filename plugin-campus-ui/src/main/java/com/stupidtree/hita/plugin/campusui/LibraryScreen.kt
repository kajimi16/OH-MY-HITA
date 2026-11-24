package com.stupidtree.hita.plugin.campusui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stupidtree.hita.api.CampusLibrarySnapshot
import com.stupidtree.hita.api.CampusService
import com.stupidtree.hita.api.CampusServiceTarget

/**
 * 图书馆智慧墙：在馆人数、今日入馆、空间占用与空闲。
 *
 * 前置条件：service 来自宿主；refreshToken 变化或点刷新时重查。
 * 后置条件：一次快照渲染四个指标与抓取时间。
 * 不变式：占用率在总数为 0 时显示 0，不伪造数据。
 * 违约处理：失败保留 CampusResult 原文并给出重试；缺会话给登录入口。
 */
@Composable
internal fun LibraryScreen(
    service: CampusService,
    refreshToken: Int,
    onLoginRequired: (CampusServiceTarget) -> Unit,
) {
    var state by remember(service) {
        mutableStateOf<LoadState<CampusLibrarySnapshot>>(LoadState.Loading)
    }
    var reload by remember(service) { mutableIntStateOf(0) }

    LaunchedEffect(service, refreshToken, reload) {
        state = LoadState.Loading
        state = service.librarySmartWall().asLoadState()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (val current = state) {
            is LoadState.Loading -> item(key = "loading") { LoadingBox() }
            is LoadState.Failed -> item(key = "failed") {
                FailureCard(
                    state = current,
                    onRetry = { reload += 1 },
                    onLogin = current.loginTarget?.let { target -> { onLoginRequired(target) } },
                )
            }
            is LoadState.Ready<CampusLibrarySnapshot> -> {
                val snapshot = current.value
                item(key = "people") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard("当前在馆", snapshot.currentInLibrary.toString(), Modifier.weight(1f))
                        MetricCard("今日入馆", snapshot.todayVisitors.toString(), Modifier.weight(1f))
                    }
                }
                item(key = "rooms") {
                    RoomsCard(snapshot)
                }
                item(key = "fetched") {
                    Text(
                        "更新于 ${formatTimestamp(snapshot.fetchedAtMillis, "HH:mm")} · 数据来自图书馆智慧墙",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "人",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun RoomsCard(snapshot: CampusLibrarySnapshot) {
    val occupied = snapshot.occupiedRooms
    val available = snapshot.availableRooms
    val total = occupied + available
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("静音仓占用", style = MaterialTheme.typography.titleMedium)
            Text(
                "已用 $occupied 间 · 可用 $available 间",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { if (total > 0) occupied.toFloat() / total else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                if (total > 0) "占用率 ${Math.round(occupied * 100f / total)}%" else "暂无空间数据",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
