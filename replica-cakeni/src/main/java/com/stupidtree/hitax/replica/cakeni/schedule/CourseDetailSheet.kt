package com.stupidtree.hitax.replica.cakeni.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.replica.cakeni.CakeniCourse
import com.stupidtree.hitax.replica.cakeni.CakeniCourseColors
import com.stupidtree.hitax.replica.cakeni.CakeniDimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WEEKDAY_FULL = arrayOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

/**
 * 课程详情底部弹窗。
 *
 * 前置条件：[week] 是当前查看的周次；[course] 只携带 UI 字段。
 * 后置条件：展示当前查看周的周次、星期、节次、时间、教师、教室，不展示课程的完整周次范围。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailSheet(course: CakeniCourse, week: Int, onDismiss: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accent = remember(course.id, course.color) {
        if (course.color != 0) Color(course.color) else CakeniCourseColors.colorFor(course.id)
    }
    val container = accent
        .copy(alpha = if (dark) 0.34f else 0.18f)
        .compositeOver(MaterialTheme.colorScheme.surface)
    val timeRange = remember(course.from, course.to) {
        "${formatTime(course.from)} - ${formatTime(course.to)}"
    }
    val slotText = if (course.startSlot == course.endSlot) {
        "第 ${course.startSlot} 节"
    } else {
        "第 ${course.startSlot}-${course.endSlot} 节"
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CakeniDimens.page)
                .padding(bottom = 20.dp)
                .navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(container),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        course.name.trim().take(1).ifBlank { "课" },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                    )
                }
                Column(
                    Modifier
                        .padding(start = 14.dp)
                        .weight(1f, fill = false),
                ) {
                    Text(
                        course.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                "第 $week 周",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                        typeLabel(course.type)?.let { label ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            InfoRow(Icons.Outlined.DateRange, "当前周", "第 $week 周")
            InfoRow(
                Icons.Outlined.Today,
                "星期",
                WEEKDAY_FULL.getOrElse(course.dayIndex) { "" },
            )
            InfoRow(Icons.Outlined.Numbers, "节次", slotText)
            InfoRow(Icons.Outlined.Schedule, "时间", timeRange)
            InfoRow(Icons.Outlined.Person, "教师", course.teacher.ifBlank { "未填写" })
            InfoRow(Icons.Outlined.Place, "教室", course.place.ifBlank { "未填写" })
            Spacer(Modifier.height(18.dp))
            FilledTonalButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("关闭")
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun typeLabel(type: EventItem.TYPE): String? = when (type) {
    EventItem.TYPE.CLASS -> null
    EventItem.TYPE.EXAM -> "考试"
    EventItem.TYPE.OTHER -> "其他"
    EventItem.TYPE.TAG -> "标签"
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(timestamp))
