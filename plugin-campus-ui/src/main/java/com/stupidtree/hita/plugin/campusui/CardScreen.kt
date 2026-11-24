package com.stupidtree.hita.plugin.campusui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stupidtree.hita.api.CampusCardHistoryRange
import com.stupidtree.hita.api.CampusCardSummary
import com.stupidtree.hita.api.CampusCardTransaction
import com.stupidtree.hita.api.CampusCardTransactions
import com.stupidtree.hita.api.CampusService
import com.stupidtree.hita.api.CampusServiceTarget

private const val PAGE_SIZE = 20

/**
 * 校园卡：余额、消费汇总、分页流水与时间范围筛选。
 *
 * 前置条件：service 来自宿主且调用在主线程；refreshToken 变化时重新查询。
 * 后置条件：摘要与流水各自独立加载，失败各自提供重试。
 * 不变式：翻页与切范围只重查流水，不重查摘要；页码夹在 1..maxPage。
 * 违约处理：任何 CampusResult 失败都保留原文并给出重试；缺会话额外给登录入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CardScreen(
    service: CampusService,
    refreshToken: Int,
    onLoginRequired: (CampusServiceTarget) -> Unit,
) {
    var summary by remember(service) {
        mutableStateOf<LoadState<CampusCardSummary>>(LoadState.Loading)
    }
    var transactions by remember(service) {
        mutableStateOf<LoadState<CampusCardTransactions>>(LoadState.Loading)
    }
    var range by remember(service) { mutableStateOf(CampusCardHistoryRange.MONTH) }
    var page by remember(service) { mutableIntStateOf(1) }
    var summaryReload by remember(service) { mutableIntStateOf(0) }
    var txReload by remember(service) { mutableIntStateOf(0) }

    LaunchedEffect(service, refreshToken, summaryReload) {
        summary = LoadState.Loading
        summary = service.cardSummary().asLoadState()
    }
    LaunchedEffect(service, refreshToken, range, page, txReload) {
        transactions = LoadState.Loading
        transactions = service.cardTransactions(page = page, pageSize = PAGE_SIZE, range = range)
            .asLoadState()
    }

    val total = (transactions as? LoadState.Ready<CampusCardTransactions>)?.value?.total ?: 0
    val maxPage = if (total <= 0) 1 else (total + PAGE_SIZE - 1) / PAGE_SIZE
    LaunchedEffect(maxPage) {
        if (page > maxPage) page = maxPage
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "summary") {
            SummarySection(
                state = summary,
                onRetry = { summaryReload += 1 },
                onLogin = { onLoginRequired(CampusServiceTarget.CARD) },
            )
        }
        val signedOut = (summary as? LoadState.Failed)?.loginTarget != null
        if (!signedOut) {
            item(key = "range") {
                RangeChips(range = range, onSelect = { range = it; page = 1 })
            }
        }
        when (val current = transactions) {
            is LoadState.Loading -> item(key = "tx-loading") { LoadingRow("正在读取消费流水…") }
            is LoadState.Failed -> if (!signedOut) {
                item(key = "tx-failed") {
                    FailureCard(
                        state = current,
                        onRetry = { txReload += 1 },
                        onLogin = current.loginTarget?.let { target -> { onLoginRequired(target) } },
                    )
                }
            }
            is LoadState.Ready<CampusCardTransactions> -> {
                val value = current.value
                if (value.items.isEmpty()) {
                    item(key = "tx-empty") {
                        Text(
                            "该范围内没有消费流水。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(value.items, key = { it.id }) { transaction ->
                        TransactionRow(transaction)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                item(key = "tx-page") {
                    PaginationRow(
                        page = page,
                        maxPage = maxPage,
                        total = value.total,
                        onPrevious = { if (page > 1) page -= 1 },
                        onNext = { if (page < maxPage) page += 1 },
                    )
                }
            }
        }
    }
}

@Composable
private fun SummarySection(
    state: LoadState<CampusCardSummary>,
    onRetry: () -> Unit,
    onLogin: () -> Unit,
) {
    when (state) {
        is LoadState.Loading -> LoadingRow("正在读取余额…")
        is LoadState.Failed -> FailureCard(
            state = state,
            onRetry = onRetry,
            onLogin = state.loginTarget?.let { { onLogin() } },
        )
        is LoadState.Ready<CampusCardSummary> -> BalanceCard(state.value)
    }
}

@Composable
private fun BalanceCard(summary: CampusCardSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("余额", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatYuan(summary.balanceCents),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
            )
            summary.statusText?.takeIf { it.isNotBlank() }?.let {
                Text("状态：$it", style = MaterialTheme.typography.bodySmall)
            }
            summary.processedAtText?.takeIf { it.isNotBlank() }?.let {
                Text(
                    "更新时间：$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                LabeledValue("昨日", summary.spending?.yesterdayText ?: "—")
                LabeledValue("本周", summary.spending?.weekText ?: "—")
                LabeledValue("本月", summary.spending?.monthText ?: "—")
            }
        }
    }
}

@Composable
private fun RangeChips(range: CampusCardHistoryRange, onSelect: (CampusCardHistoryRange) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            CampusCardHistoryRange.MONTH to "本月",
            CampusCardHistoryRange.YEAR to "本年",
            CampusCardHistoryRange.ALL to "全部",
        ).forEach { (value, label) ->
            FilterChip(
                selected = value == range,
                onClick = { onSelect(value) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun TransactionRow(transaction: CampusCardTransaction) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                transaction.merchant.ifBlank { "未知商户" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOf(transaction.occurredAtText, transaction.method)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            formatSignedYuan(transaction.amountCents),
            style = MaterialTheme.typography.titleSmall,
            color = if (transaction.amountCents < 0) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}

@Composable
private fun PaginationRow(
    page: Int,
    maxPage: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrevious, enabled = page > 1) { Text("上一页") }
        Text(
            "第 $page / $maxPage 页 · 共 $total 条",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onNext, enabled = page < maxPage) { Text("下一页") }
    }
}

@Composable
private fun LoadingRow(text: String) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}
