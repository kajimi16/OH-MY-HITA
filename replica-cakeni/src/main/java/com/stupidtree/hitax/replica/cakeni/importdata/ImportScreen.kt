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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stupidtree.hita.api.EasAuthService
import com.stupidtree.hita.api.EasService
import com.stupidtree.hitax.replica.cakeni.CakeniDimens

/**
 * 导入页：教务账号、API 登录、导入课表、文件导入说明、插件市场。
 *
 * 前置条件：easAuth/easService 可为空；回调由宿主 Shell 提供。
 * 后置条件：登录只调用 EasAuthService，不保存密码；导入调用宿主 EasService。
 * 不变式：本页不直接发网络请求，不写课表数据库。
 */
@Composable
fun ImportScreen(
    easAuth: EasAuthService?,
    easService: EasService?,
    onImport: () -> Unit,
    onWebLogin: () -> Unit,
    onOpenMarket: () -> Unit,
) {
    var loggedIn by remember(easAuth) { mutableStateOf(easAuth?.isLogin() == true) }
    var accountName by remember(easAuth) { mutableStateOf(easAuth?.accountName()) }
    var accountSubtitle by remember(easAuth) { mutableStateOf(easAuth?.accountSubtitle()) }

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
            loggedIn = loggedIn,
            accountName = accountName,
            accountSubtitle = accountSubtitle,
            canLogin = easAuth != null,
            canImport = easService != null && loggedIn,
            onImport = onImport,
            onWebLogin = onWebLogin,
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


}

@Composable
private fun AcademicAccountCard(
    loggedIn: Boolean,
    accountName: String?,
    accountSubtitle: String?,
    canLogin: Boolean,
    canImport: Boolean,
    onImport: () -> Unit,
    onWebLogin: () -> Unit,
) {
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
                            loggedIn -> accountName?.takeIf { it.isNotBlank() } ?: "已登录"
                            else -> "API 登录后可直接导入课表"
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
                        text = if (loggedIn) "已登录" else "未登录",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (loggedIn) {
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
                        onClick = onWebLogin,
                        enabled = canLogin,
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("重新登录") }
                } else {
                    Button(
                        onClick = onWebLogin,
                        enabled = canLogin,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Outlined.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(7.dp))
                        Text("登录教务")
                    }
                    OutlinedButton(
                        onClick = onImport,
                        enabled = canImport,
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("导入课表") }
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
