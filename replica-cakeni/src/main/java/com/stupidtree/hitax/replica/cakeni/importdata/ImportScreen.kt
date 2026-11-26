package com.stupidtree.hitax.replica.cakeni.importdata

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.EasAuthService
import com.stupidtree.hita.api.EasSessionService
import com.stupidtree.hitax.replica.cakeni.CakeniDimens

/**
 * 导入页：教务账号、API 登录、导入课表、文件导入说明、插件市场。
 *
 * 前置条件：easAuth/easSession 可为空；回调由宿主 Shell 提供。
 * 后置条件：登录只调用 EasAuthService，不保存密码；导入由宿主通过 EasSessionService 触发。
 * 不变式：本页不直接发网络请求，不写课表数据库。
 */
@Composable
fun ImportScreen(
    easAuth: EasAuthService?,
    easSession: EasSessionService?,
    onImport: () -> Unit,
    onWebLogin: () -> Unit,
    onApiLoginSuccess: () -> Unit,
    onOpenMarket: () -> Unit,
) {
    var apiReady by remember(easAuth) { mutableStateOf(easAuth?.hasApiToken() == true) }
    var accountName by remember(easAuth) { mutableStateOf(easAuth?.accountName()) }
    var accountSubtitle by remember(easAuth) { mutableStateOf(easAuth?.accountSubtitle()) }
    var showApiLogin by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = CakeniDimens.page)
            .padding(top = CakeniDimens.page, bottom = 24.dp),
    ) {
        Text(
            text = "导入课程",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "先登录教务账号，再从教务系统导入本学期课表。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        AcademicAccountCard(
            accountName = accountName,
            accountSubtitle = accountSubtitle,
            apiReady = apiReady,
            canLogin = easAuth != null,
            canImport = easSession != null && apiReady,
            onImport = onImport,
            onWebLogin = onWebLogin,
            onApiLogin = { showApiLogin = true },
        )

        Spacer(Modifier.height(22.dp))
        SectionLabel("其他导入方式")
        Spacer(Modifier.height(10.dp))
        FileImportCard()

        Spacer(Modifier.height(22.dp))
        SectionLabel("扩展")
        Spacer(Modifier.height(10.dp))
        MarketEntryCard(onOpenMarket = onOpenMarket)

        Spacer(Modifier.height(22.dp))
        ImportTipCard()
    }

    if (showApiLogin && easAuth != null) {
        ApiLoginDialog(
            auth = easAuth,
            onDismiss = { showApiLogin = false },
            onSuccess = {
                showApiLogin = false
                apiReady = true
                accountName = easAuth.accountName()
                accountSubtitle = easAuth.accountSubtitle()
                onApiLoginSuccess()
            },
        )
    }
}

@Composable
private fun AcademicAccountCard(
    apiReady: Boolean,
    accountName: String?,
    accountSubtitle: String?,
    canLogin: Boolean,
    canImport: Boolean,
    onImport: () -> Unit,
    onWebLogin: () -> Unit,
    onApiLogin: () -> Unit,
) {
    val loggedIn = canLogin && (apiReady || accountName != null)
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 170.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = if (loggedIn) Icons.Outlined.CheckCircle else Icons.Outlined.School,
                    container = MaterialTheme.colorScheme.primary,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    size = 56.dp,
                    iconSize = 28.dp,
                )
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        text = "教务账号",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = when {
                            !canLogin -> "教务服务未安装"
                            !apiReady && accountName != null -> "网页会话已连接，导入需账号密码登录"
                            apiReady -> accountName?.takeIf { it.isNotBlank() } ?: "已登录"
                            else -> "账号密码登录后可直接导入课表"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f),
                    )
                    accountSubtitle?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (loggedIn) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    contentColor = if (loggedIn) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text(
                        text = if (apiReady) "已登录" else "未登录",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (apiReady) {
                    Button(
                        onClick = onImport,
                        enabled = canImport,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(7.dp))
                        Text("导入课表")
                    }
                    OutlinedButton(
                        onClick = onApiLogin,
                        enabled = canLogin,
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("重新登录") }
                } else {
                    Button(
                        onClick = onApiLogin,
                        enabled = canLogin,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Outlined.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(7.dp))
                        Text("账号密码登录")
                    }
                    OutlinedButton(
                        onClick = onWebLogin,
                        enabled = canLogin,
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("网页登录") }
                }
            }
        }
    }
}

@Composable
private fun FileImportCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { disabled() },
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                icon = Icons.Outlined.Description,
                container = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                size = 48.dp,
                iconSize = 24.dp,
            )
            Column(
                modifier = Modifier.weight(1f).padding(start = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "ICS / 文件导入",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "支持 ICS 与课表文件，解析在宿主页面完成。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
            Spacer(Modifier.size(10.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Text(
                    text = "使用宿主导入入口",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun MarketEntryCard(onOpenMarket: () -> Unit) {
    Surface(
        onClick = onOpenMarket,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                icon = Icons.Outlined.Storefront,
                container = MaterialTheme.colorScheme.secondaryContainer,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                size = 44.dp,
                iconSize = 22.dp,
            )
            Column(
                modifier = Modifier.weight(1f).padding(start = 14.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = "插件市场",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "安装或管理导入相关的课表插件。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ImportTipCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.padding(start = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "导入说明",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "登录成功后点“导入课表”，由宿主解析并写入；完成后回到课表查看。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "文件导入暂无本页回调，请使用宿主导入入口操作。",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    container: Color,
    tint: Color,
    size: Dp,
    iconSize: Dp,
) {
    Box(
        modifier = Modifier.size(size).background(container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize), tint = tint)
    }
}


@Composable
private fun ApiLoginDialog(
    auth: EasAuthService,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var request by remember { mutableStateOf<LiveData<DataState<Boolean>>?>(null) }
    val fallback = remember { MutableLiveData<DataState<Boolean>>() }
    val result by (request ?: fallback).observeAsState()
    LaunchedEffect(result) {
        val state = result ?: return@LaunchedEffect
        if (state.state == DataState.STATE.SUCCESS && state.data == true) {
            onSuccess()
        } else {
            error = state.message?.takeIf { it.isNotBlank() } ?: "登录失败，请检查账号密码"
            loading = false
        }
    }
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("账号密码登录") },
        text = {
            Column {
                Text("网页会话只能用于网页接口，导入课表需要教务 API 登录。")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("学号") },
                    singleLine = true,
                    enabled = !loading,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    singleLine = true,
                    enabled = !loading,
                    visualTransformation = PasswordVisualTransformation(),
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    loading = true
                    error = null
                    request = auth.login(username.trim(), password)
                },
                enabled = !loading && username.isNotBlank() && password.isNotBlank(),
            ) { Text(if (loading) "登录中…" else "登录") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) { Text("取消") }
        },
    )
}
