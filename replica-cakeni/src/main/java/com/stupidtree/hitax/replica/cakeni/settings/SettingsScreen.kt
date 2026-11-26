package com.stupidtree.hitax.replica.cakeni.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stupidtree.hitax.replica.cakeni.CakeniDimens
import com.stupidtree.hitax.replica.cakeni.R

private const val REPO_URL = "github.com/cakeni/CourseSchedule"

private enum class AppearanceOption(val label: String, val value: Boolean?) {
    System("跟随系统", null),
    Light("浅色", false),
    Dark("深色", true),
}

/**
 * 设置页。
 *
 * 前置条件：回调由 Shell 持有，状态由 Shell 的 rememberSaveable 保存。
 * 后置条件：三个开关与外观选择只通过回调改变 Shell 展示状态；本页不读写持久化存储。
 */
@Composable
fun SettingsScreen(
    darkTheme: Boolean?,
    onDarkThemeChange: (Boolean?) -> Unit,
    showWeekend: Boolean,
    onShowWeekendChange: (Boolean) -> Unit,
    showNonCurrentWeek: Boolean,
    onShowNonCurrentWeekChange: (Boolean) -> Unit,
    showPeriodTime: Boolean,
    onShowPeriodTimeChange: (Boolean) -> Unit,
    onOpenMarket: () -> Unit,
) {
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = CakeniDimens.page, vertical = 8.dp),
    ) {
        Text(
            stringResource(R.string.cakeni_settings_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(14.dp))
        AnnouncementCard()
        Spacer(Modifier.height(18.dp))

        SectionTitle("外观")
        SettingCard {
            AppearanceSelector(current = darkTheme, onSelect = onDarkThemeChange)
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("课表显示")
        SettingCard {
            ToggleRow("显示周末", "在课表网格中展示周六与周日", showWeekend, onShowWeekendChange)
            SettingsDivider()
            ToggleRow("显示非本周课程", "展示不属于当前查看周的课程", showNonCurrentWeek, onShowNonCurrentWeekChange)
            SettingsDivider()
            ToggleRow("显示节次时间", "在左侧时间轴显示每节课的上课时间", showPeriodTime, onShowPeriodTimeChange)
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle("更多")
        SettingCard {
            EntryRow(Icons.Outlined.DateRange, "学期管理", "查看与切换当前学期") { dialog = "semester" }
            SettingsDivider()
            EntryRow(Icons.Outlined.Storefront, "插件市场", "浏览并启用更多插件") { onOpenMarket() }
            SettingsDivider()
            EntryRow(Icons.Outlined.Info, "关于", "版本信息与开源许可") { dialog = "about" }
        }
        Spacer(Modifier.height(24.dp))
    }

    when (dialog) {
        "semester" -> InfoDialog(
            title = "学期管理",
            body = "学期数据由宿主课表服务提供。此处仅展示入口，暂不支持在本页新建或切换学期。",
            onDismiss = { dialog = null },
        )
        "about" -> InfoDialog(
            title = "关于",
            body = "HITA cakeni 风格复刻界面。\n界面参考 cakeni/CourseSchedule，遵循 MIT License。\n仓库：$REPO_URL",
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun AnnouncementCard() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(CakeniDimens.cardRadius),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("项目公告", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                "本界面为 cakeni/CourseSchedule 的 Compose 复刻，界面与交互参考原项目。",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "开源许可：MIT License",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                "仓库地址：$REPO_URL",
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(CakeniDimens.cardRadius),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    )
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun AppearanceSelector(current: Boolean?, onSelect: (Boolean?) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (option in AppearanceOption.entries) {
            val selected = current == option.value
            Surface(
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(option.value) },
            ) {
                Text(
                    option.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun EntryRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } },
        title = { Text(title) },
        text = { Text(body) },
    )
}
