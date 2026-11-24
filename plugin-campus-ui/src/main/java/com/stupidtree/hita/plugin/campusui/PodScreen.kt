package com.stupidtree.hita.plugin.campusui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stupidtree.hita.api.CampusPodCategory
import com.stupidtree.hita.api.CampusPodContacts
import com.stupidtree.hita.api.CampusPodGroup
import com.stupidtree.hita.api.CampusPodReservation
import com.stupidtree.hita.api.CampusPodReservationRequest
import com.stupidtree.hita.api.CampusPodSpace
import com.stupidtree.hita.api.CampusResult
import com.stupidtree.hita.api.CampusService
import com.stupidtree.hita.api.CampusServiceTarget
import kotlinx.coroutines.launch

/**
 * 静音仓：按日期看分组空间，选时段创建预约，管理已有预约。
 *
 * 前置条件：service 来自宿主；refreshToken 变化或写操作成功后重新查询。
 * 后置条件：空间与预约各自独立加载；预约/取消成功后两侧都刷新。
 * 不变式：分组默认折叠；日期只影响空间查询，不影响我的预约。
 * 违约处理：失败保留原文并给重试；缺会话给登录入口；写失败留在对话框内提示。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PodScreen(
    service: CampusService,
    refreshToken: Int,
    onLoginRequired: (CampusServiceTarget) -> Unit,
    onMessage: (String) -> Unit,
) {
    var date by remember(service) { mutableStateOf(todayDateText()) }
    var groups by remember(service) {
        mutableStateOf<LoadState<List<CampusPodGroup>>>(LoadState.Loading)
    }
    var reservations by remember(service) {
        mutableStateOf<LoadState<List<CampusPodReservation>>>(LoadState.Loading)
    }
    var groupsReload by remember(service) { mutableIntStateOf(0) }
    var reservationsReload by remember(service) { mutableIntStateOf(0) }
    var expandedGroups by remember(service) { mutableStateOf(emptySet<CampusPodCategory>()) }
    var reservingSpace by remember(service) { mutableStateOf<CampusPodSpace?>(null) }
    var cancellingReservation by remember(service) { mutableStateOf<CampusPodReservation?>(null) }


    LaunchedEffect(service, refreshToken, date, groupsReload) {
        groups = LoadState.Loading
        groups = service.podSpaces(date).asLoadState()
    }
    LaunchedEffect(service, refreshToken, reservationsReload) {
        reservations = LoadState.Loading
        reservations = service.podReservations().asLoadState()
    }
    reservingSpace?.let { space ->
        ReservationDialog(
            service = service,
            space = space,
            date = date,
            refreshToken = refreshToken,
            onLoginRequired = onLoginRequired,
            onDismiss = { reservingSpace = null },
            onCreated = { id ->
                reservingSpace = null
                onMessage(if (id != null) "预约成功，编号 $id" else "预约成功")
                groupsReload += 1
                reservationsReload += 1
            },
        )
    }
    cancellingReservation?.let { reservation ->
        CancelDialog(
            service = service,
            reservation = reservation,
            onDismiss = { cancellingReservation = null },
            onCancelled = {
                cancellingReservation = null
                onMessage("已取消预约")
                groupsReload += 1
                reservationsReload += 1
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val signedOut = (groups as? LoadState.Failed)?.loginTarget != null
        if (!signedOut) {
            item(key = "date") { DateBar(date = date, onDateChange = { date = it }) }
        }
        when (val current = groups) {
            is LoadState.Loading -> item(key = "spaces-loading") { PodLoading("正在读取空间…") }
            is LoadState.Failed -> item(key = "spaces-failed") {
                FailureCard(
                    state = current,
                    onRetry = { groupsReload += 1 },
                    onLogin = current.loginTarget?.let { target -> { onLoginRequired(target) } },
                )
            }
            is LoadState.Ready<List<CampusPodGroup>> -> {
                if (current.value.isEmpty()) {
                    item(key = "spaces-empty") {
                        Text(
                            "这一天没有可预约的空间。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                current.value.forEach { group ->
                    val expanded = group.category in expandedGroups
                    item(key = "group-${group.category.name}") {
                        SectionHeader(
                            title = categoryLabel(group.category),
                            count = group.spaces.size,
                            expanded = expanded,
                            onToggle = {
                                expandedGroups = if (expanded) {
                                    expandedGroups - group.category
                                } else {
                                    expandedGroups + group.category
                                }
                            },
                        )
                    }
                    if (expanded) {
                        items(group.spaces, key = { "space-${group.category.name}-${it.id}" }) { space ->
                            PodSpaceCard(space = space, onReserve = { reservingSpace = space })
                        }
                    }
                }
            }
        }
        if (!signedOut) {
        item(key = "reservations-header") {
            SectionHeader(
                title = "我的预约",
                count = (reservations as? LoadState.Ready<List<CampusPodReservation>>)?.value?.size,
            )
        }
        when (val current = reservations) {
            is LoadState.Loading -> item(key = "res-loading") { PodLoading("正在读取预约…") }
            is LoadState.Failed -> item(key = "res-failed") {
                FailureCard(
                    state = current,
                    onRetry = { reservationsReload += 1 },
                    onLogin = current.loginTarget?.let { target -> { onLoginRequired(target) } },
                )
            }
            is LoadState.Ready<List<CampusPodReservation>> -> {
                if (current.value.isEmpty()) {
                    item(key = "res-empty") {
                        Text(
                            "还没有预约记录。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(current.value, key = { "reservation-${it.id}" }) { reservation ->
                    ReservationCard(
                        reservation = reservation,
                        onCancel = { cancellingReservation = reservation },
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun DateBar(date: String, onDateChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "日期 $date" + if (date == todayDateText()) "（今天）" else "",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onDateChange(shiftDate(date, -1)) }) { Text("前一天") }
        TextButton(onClick = { onDateChange(todayDateText()) }) { Text("今天") }
        TextButton(onClick = { onDateChange(shiftDate(date, 1)) }) { Text("后一天") }
    }
}

@Composable
private fun PodLoading(text: String) {
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
private fun PodSpaceCard(space: CampusPodSpace, onReserve: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(space.codeName, style = MaterialTheme.typography.titleMedium)
                Text(
                    space.areaFullName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (space.isAccessControl) {
                    Text(
                        "门禁",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            val meta = listOfNotNull(
                space.spaceTypeName,
                if (space.isTeam) "容量 ${space.capacity}（同行 ${space.teamCapacity - 1}）"
                else "容量 ${space.capacity}",
                space.minDurationMinutes?.let { "最短 $it 分钟" },
                space.maxDurationMinutes?.let { "最长 $it 分钟" },
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val windows = space.allowDurations
            Text(
                if (windows.isEmpty()) "今天没有可约窗口" else "可约窗口 ${windows.joinToString("  ")}",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (windows.isNotEmpty()) {
                val guestRange = space.requiredGuestRange
                if (guestRange != null) {
                    Text(
                        "多人仓：需同行 ${guestRange.first}-${guestRange.last} 人，预约时选择同行成员。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = onReserve,
                    modifier = Modifier.align(Alignment.End),
                ) { Text(if (guestRange != null) "预约（多人）" else "预约") }
            }
        }
    }
}

private fun categoryLabel(category: CampusPodCategory): String = when (category) {
    CampusPodCategory.QUAD_LOCKED -> "四人仓（门禁）"
    CampusPodCategory.QUAD -> "四人仓"
    CampusPodCategory.SINGLE -> "单人仓"
}

/**
 * 预约对话框：开始 / 结束时间都用服务端的纯计算结果做选项。
 *
 * 契约：选项在对话框打开时按“当前时刻”算一次；提交失败时保留对话框并显示失败原因。
 * 多人仓（[CampusPodSpace.requiredGuestRange] 非空）先加载同行成员，
 * 选中数量落在范围内才允许提交；添加成员先核验读者证号，再加入并保存。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReservationDialog(
    service: CampusService,
    space: CampusPodSpace,
    date: String,
    refreshToken: Int,
    onLoginRequired: (CampusServiceTarget) -> Unit,
    onDismiss: () -> Unit,
    onCreated: (Int?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val nowMinutes = remember(space, date) {
        if (date == todayDateText()) nowMinutesOfDay() else 0
    }
    val startOptions = remember(space, nowMinutes) { service.podStartOptions(space, nowMinutes) }
    var start by remember(space, nowMinutes) { mutableStateOf(startOptions.firstOrNull()) }
    val endOptions = remember(space, start) {
        start?.let { service.podEndOptions(space, it) } ?: emptyList()
    }
    var end by remember(space, start) { mutableStateOf<Int?>(null) }
    val chosenEnd = end?.takeIf { it in endOptions }
        ?: endOptions.firstOrNull { candidate -> start != null && candidate - start!! >= 60 }
        ?: endOptions.firstOrNull()
    var notes by remember(space) { mutableStateOf("") }
    var busy by remember(space) { mutableStateOf(false) }
    var error by remember(space) { mutableStateOf<String?>(null) }
    val guestRange = space.requiredGuestRange
    var selectedContactIds by remember(space) { mutableStateOf(emptySet<Int>()) }
    val guestSelectionValid = guestRange == null || selectedContactIds.size in guestRange

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("预约 ${space.codeName}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "日期 $date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (startOptions.isEmpty()) {
                    Text("今天已没有可预约的时段。", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("开始时间", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(startOptions) { minute ->
                            FilterChip(
                                selected = minute == start,
                                onClick = { start = minute; end = null },
                                label = { Text(formatMinutes(minute)) },
                            )
                        }
                    }
                    Text("结束时间", style = MaterialTheme.typography.labelLarge)
                    if (endOptions.isEmpty()) {
                        Text(
                            "该开始时间没有合适的结束时间。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(endOptions) { minute ->
                                FilterChip(
                                    selected = minute == chosenEnd,
                                    onClick = { end = minute },
                                    label = { Text(formatMinutes(minute)) },
                                )
                            }
                        }
                    }
                    if (start != null && chosenEnd != null) {
                        Text(
                            "时段 ${formatMinutes(start!!)}-${formatMinutes(chosenEnd)}（${chosenEnd - start!!} 分钟）",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                if (guestRange != null) {
                    TeamGuestSection(
                        service = service,
                        guestRange = guestRange,
                        refreshToken = refreshToken,
                        selectedIds = selectedContactIds,
                        onSelectedChange = { selectedContactIds = it },
                        onLoginRequired = { target ->
                            onDismiss()
                            onLoginRequired(target)
                        },
                        enabled = !busy,
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(120) },
                    label = { Text("备注（可选）") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && start != null && chosenEnd != null && guestSelectionValid,
                onClick = {
                    val startMinute = start
                    val endMinute = chosenEnd
                    if (startMinute == null || endMinute == null) return@TextButton
                    scope.launch {
                        busy = true
                        error = null
                        val result = service.createPodReservation(
                            CampusPodReservationRequest(
                                spaceId = space.id,
                                date = date,
                                startTime = formatMinutes(startMinute),
                                endTime = formatMinutes(endMinute),
                                notes = notes.trim(),
                                contactIds = if (guestRange != null) {
                                    selectedContactIds.toList()
                                } else {
                                    emptyList()
                                },
                            )
                        )
                        busy = false
                        when (result) {
                            is CampusResult.Success -> onCreated(result.value)
                            else -> error = result.failureMessageOrNull()
                        }
                    }
                },
            ) { Text(if (busy) "提交中…" else "提交") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") }
        },
    )
}

/**
 * 多人仓的同行成员：加载已保存成员、勾选、按证号核验后添加、删除。
 *
 * 前置条件：guestRange 来自 [CampusPodSpace.requiredGuestRange]，非空。
 * 后置条件：选中集合只含已保存成员，且不超过 guestRange.last；数量不足时由调用方禁用提交。
 * 不变式：添加/删除期间禁用手动操作；服务端失败保留对话框并显示失败原因。
 * 违约处理：NotSignedIn / Network / Remote / Parse 按 [CampusResult] 原文展示；加载失败且缺会话时给登录入口。
 */
@Composable
private fun TeamGuestSection(
    service: CampusService,
    guestRange: IntRange,
    refreshToken: Int,
    selectedIds: Set<Int>,
    onSelectedChange: (Set<Int>) -> Unit,
    onLoginRequired: (CampusServiceTarget) -> Unit,
    enabled: Boolean,
) {
    val scope = rememberCoroutineScope()
    var contactsState by remember(service) {
        mutableStateOf<LoadState<CampusPodContacts>>(LoadState.Loading)
    }
    var reload by remember(service) { mutableIntStateOf(0) }
    var nameInput by remember(service) { mutableStateOf("") }
    var cardNoInput by remember(service) { mutableStateOf("") }
    var guestBusy by remember(service) { mutableStateOf(false) }
    var actionError by remember(service) { mutableStateOf<String?>(null) }
    var info by remember(service) { mutableStateOf<String?>(null) }
    var pendingRemoveId by remember(service) { mutableStateOf<Int?>(null) }

    LaunchedEffect(service, refreshToken, reload) {
        contactsState = LoadState.Loading
        val loaded = service.podContacts()
        contactsState = loaded.asLoadState()
        if (loaded is CampusResult.Success) {
            val known = loaded.value.contacts.mapTo(HashSet()) { it.id }
            val pruned = selectedIds.filter { it in known }.toSet()
            if (pruned != selectedIds) onSelectedChange(pruned)
        }
    }

    val selectedCount = selectedIds.size
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "同行成员",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                "已选 $selectedCount / ${guestRange.first}-${guestRange.last}",
                style = MaterialTheme.typography.labelMedium,
                color = if (selectedCount in guestRange) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
        Text(
            "需选择 ${guestRange.first}-${guestRange.last} 位同行成员（不含本人）。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (selectedCount < guestRange.first) {
            Text(
                "还需选择 ${guestRange.first - selectedCount} 位同行成员才能提交。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        } else if (selectedCount > guestRange.last) {
            Text(
                "已超出上限，请取消 ${selectedCount - guestRange.last} 位同行成员。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        when (val current = contactsState) {
            is LoadState.Loading -> PodLoading("正在读取同行成员…")
            is LoadState.Failed -> FailureCard(
                state = current,
                onRetry = { reload += 1 },
                onLogin = current.loginTarget?.let { target -> { onLoginRequired(target) } },
                loginButtonFullWidth = false,
            )
            is LoadState.Ready<CampusPodContacts> -> {
                val contacts = current.value.contacts
                if (contacts.isEmpty()) {
                    Text(
                        "还没有保存的同行成员，先在下方添加。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                contacts.forEach { contact ->
                    val selected = contact.id in selectedIds
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = selected,
                            enabled = enabled && !guestBusy &&
                                (selected || selectedIds.size < guestRange.last),
                            onClick = {
                                val next = if (selected) {
                                    selectedIds - contact.id
                                } else {
                                    selectedIds + contact.id
                                }
                                onSelectedChange(next)
                                actionError = null
                            },
                            label = {
                                Text(
                                    contact.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                        Text(
                            contact.cardNo.ifBlank { "无证号" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(
                            enabled = enabled && !guestBusy,
                            onClick = {
                                pendingRemoveId = contact.id
                                actionError = null
                                info = null
                            },
                        ) { Text("删除") }
                    }
                    if (pendingRemoveId == contact.id) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "确认删除「${contact.name}」？",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                enabled = enabled && !guestBusy,
                                onClick = {
                                    scope.launch {
                                        guestBusy = true
                                        actionError = null
                                        info = null
                                        val result = service.removePodContact(contact.id)
                                        when (result) {
                                            is CampusResult.Success -> {
                                                val ready = contactsState as? LoadState.Ready
                                                if (ready != null) {
                                                    contactsState = LoadState.Ready(
                                                        ready.value.copy(
                                                            contacts = ready.value.contacts
                                                                .filterNot { it.id == contact.id },
                                                        ),
                                                    )
                                                }
                                                onSelectedChange(selectedIds - contact.id)
                                                pendingRemoveId = null
                                                info = "已删除同行成员「${contact.name}」。"
                                            }
                                            else -> actionError = result.failureMessageOrNull()
                                        }
                                        guestBusy = false
                                    }
                                },
                            ) { Text(if (guestBusy) "删除中…" else "确认删除") }
                            TextButton(
                                enabled = !guestBusy,
                                onClick = { pendingRemoveId = null },
                            ) { Text("取消") }
                        }
                    }
                }
                Text("添加同行成员", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it.take(30) },
                    label = { Text("姓名") },
                    singleLine = true,
                    enabled = enabled && !guestBusy,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = cardNoInput,
                    onValueChange = { cardNoInput = it.take(30) },
                    label = { Text("读者证号") },
                    singleLine = true,
                    enabled = enabled && !guestBusy,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    enabled = enabled && !guestBusy &&
                        nameInput.isNotBlank() && cardNoInput.isNotBlank(),
                    onClick = {
                        val name = nameInput.trim()
                        val cardNo = cardNoInput.trim()
                        if (contacts.any { it.cardNo == cardNo }) {
                            actionError = "读者证号 $cardNo 已在同行成员列表中。"
                        } else {
                            scope.launch {
                                guestBusy = true
                                actionError = null
                                info = null
                                val verified = service.verifyPodReader(cardNo)
                                when (verified) {
                                    is CampusResult.Success -> {
                                        val reader = verified.value
                                        if (reader == null) {
                                            actionError = "读者证号 $cardNo 查不到对应读者，请核对后重试。"
                                        } else if (reader.name.trim() != name) {
                                            actionError =
                                                "证号 $cardNo 对应的姓名是「${reader.name}」，与输入的「$name」不一致。"
                                        } else {
                                            val resolvedCardNo = reader.cardNo.trim().ifBlank { cardNo }
                                            val added = service.addPodContact(
                                                name = reader.name.trim(),
                                                cardNo = resolvedCardNo,
                                            )
                                            when (added) {
                                                is CampusResult.Success -> {
                                                    val contact = added.value
                                                    val ready = contactsState as? LoadState.Ready
                                                    if (ready != null) {
                                                        val others = ready.value.contacts.filterNot {
                                                            it.id == contact.id ||
                                                                (contact.cardNo.isNotBlank() &&
                                                                    it.cardNo == contact.cardNo)
                                                        }
                                                        contactsState = LoadState.Ready(
                                                            ready.value.copy(contacts = others + contact),
                                                        )
                                                    }
                                                    if (selectedIds.size < guestRange.last) {
                                                        onSelectedChange(selectedIds + contact.id)
                                                    }
                                                    nameInput = ""
                                                    cardNoInput = ""
                                                    info = "已添加并保存同行成员「${contact.name}」。"
                                                }
                                                else -> actionError = added.failureMessageOrNull()
                                            }
                                        }
                                    }
                                    else -> actionError = verified.failureMessageOrNull()
                                }
                                guestBusy = false
                            }
                        }
                    },
                ) { Text(if (guestBusy) "处理中…" else "核验并添加") }
            }
        }
        info?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        actionError?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/**
 * 取消预约：原因可选，提交后由调用方刷新列表。
 *
 * 前置条件：reservation 来自上一次查询且 isAllowCancel 为真。
 * 后置条件：成功回调 onCancelled；失败留在对话框内显示原因。
 * 不变式：提交中禁用两个按钮，避免重复取消。
 * 违约处理：CampusResult 失败原样展示，不关闭对话框。
 */
@Composable
private fun CancelDialog(
    service: CampusService,
    reservation: CampusPodReservation,
    onDismiss: () -> Unit,
    onCancelled: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var reason by remember(reservation.id) { mutableStateOf("") }
    var busy by remember(reservation.id) { mutableStateOf(false) }
    var error by remember(reservation.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("取消预约") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    listOf(reservation.spaceName, reservation.date,
                        "${reservation.startTime}-${reservation.endTime}")
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(100) },
                    label = { Text("取消原因（可选）") },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        error = null
                        val result = service.cancelPodReservation(
                            reservationId = reservation.id,
                            reason = reason.trim().ifBlank { null },
                        )
                        busy = false
                        when (result) {
                            is CampusResult.Success -> onCancelled()
                            else -> error = result.failureMessageOrNull()
                        }
                    }
                },
            ) { Text(if (busy) "提交中…" else "确认取消") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("返回") }
        },
    )
}

@Composable
private fun ReservationCard(reservation: CampusPodReservation, onCancel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    reservation.spaceName.ifBlank { "静音仓" },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    reservation.statusText.ifBlank { reservation.status },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            val detail = listOfNotNull(
                reservation.areaName.takeIf { it.isNotBlank() },
                reservation.spaceTypeName,
                reservation.typeText,
            ).joinToString(" · ")
            if (detail.isNotBlank()) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${reservation.date} ${reservation.startTime}-${reservation.endTime}",
                style = MaterialTheme.typography.bodyMedium,
            )
            val tags = listOfNotNull(
                if (reservation.isMain) "主预约" else null,
                if (reservation.isNeedSign) "需签到" else null,
            ).joinToString(" · ")
            if (tags.isNotBlank()) {
                Text(
                    tags,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            reservation.cancelReason?.takeIf { it.isNotBlank() }?.let {
                Text(
                    "取消原因：$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (reservation.isAllowCancel) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.align(Alignment.End),
                ) { Text("取消预约") }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

