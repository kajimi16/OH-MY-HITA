package com.stupidtree.hita.plugin.campusui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stupidtree.hita.api.CampusLabProgress
import com.stupidtree.hita.api.CampusLabSession
import com.stupidtree.hita.api.CampusService
import com.stupidtree.hita.api.CampusServiceTarget

/**
 * 实验中心：学期、当前周、总场次与已完成场次，以及逐节实验课列表。
 *
 * 前置条件：service 来自宿主；refreshToken 变化或点刷新时重查。
 * 后置条件：有教学周时显示已完成 / 总数，没有时只显示总数。
 * 不变式：已完成只按周次与星期比较，不猜测当天课程是否结束。
 * 违约处理：失败保留 CampusResult 原文并给出重试；缺会话给登录入口。
 */
@Composable
internal fun LabScreen(
    service: CampusService,
    refreshToken: Int,
    onLoginRequired: (CampusServiceTarget) -> Unit,
) {
    var state by remember(service) {
        mutableStateOf<LoadState<CampusLabProgress>>(LoadState.Loading)
    }
    var reload by remember(service) { mutableIntStateOf(0) }

    LaunchedEffect(service, refreshToken, reload) {
        state = LoadState.Loading
        state = service.labProgress().asLoadState()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (val current = state) {
            is LoadState.Loading -> item(key = "loading") { LabLoading("正在读取实验课进度…") }
            is LoadState.Failed -> item(key = "failed") {
                Box(
                    modifier = Modifier.fillParentMaxHeight().fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    FailureCard(
                        state = current,
                        onRetry = { reload += 1 },
                        onLogin = current.loginTarget?.let { target -> { onLoginRequired(target) } },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        loginButtonFullWidth = false,
                    )
                }
            }
            is LoadState.Ready<CampusLabProgress> -> {
                val progress = current.value
                val currentWeek = progress.currentWeek
                val completed = currentWeek?.let {
                    progress.completedCount(it, todayWeekday())
                }
                item(key = "progress") {
                    ProgressCard(
                        progress = progress,
                        completed = completed,
                    )
                }
                if (progress.sessions.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            "本学期还没有实验课安排。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(
                        progress.sessions.sortedWith(
                            compareBy<CampusLabSession>(
                                { it.week },
                                { it.weekday },
                                { it.startSection },
                            )
                        ),
                        key = { it.id },
                    ) { session ->
                        SessionRow(session)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun LabLoading(text: String) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ProgressCard(progress: CampusLabProgress, completed: Int?) {
    val total = progress.totalCount
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                progress.termName?.takeIf { it.isNotBlank() } ?: "学期未知",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                progress.currentWeek?.let { "当前第 $it 周" } ?: "当前教学周未知",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (completed != null) "已完成 $completed / 总场次 $total"
                else "总场次 $total（教学周未知，无法判断已上）",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            LinearProgressIndicator(
                progress = {
                    if (completed != null && total > 0) completed.toFloat() / total else 0f
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "“已完成”按周次与星期比较，不含当天尚未结束的场次。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SessionRow(session: CampusLabSession) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.padding(end = 12.dp)) {
            Text(
                "第 ${session.week} 周",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                weekdayLabel(session.weekday),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                session.courseName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val sections = "第 ${session.startSection}-${session.endSection} 节"
            Text(
                session.room?.takeIf { it.isNotBlank() }?.let { "$sections · $it" } ?: sections,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun weekdayLabel(weekday: Int): String =
    listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        .getOrNull(weekday - 1) ?: "星期 $weekday"

