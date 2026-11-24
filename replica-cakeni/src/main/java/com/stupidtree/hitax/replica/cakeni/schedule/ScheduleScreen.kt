package com.stupidtree.hitax.replica.cakeni.schedule

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stupidtree.hitax.replica.cakeni.CakeniBlock
import com.stupidtree.hitax.replica.cakeni.CakeniCourse
import com.stupidtree.hitax.replica.cakeni.CakeniCourseColors
import com.stupidtree.hitax.replica.cakeni.CakeniDay
import com.stupidtree.hitax.replica.cakeni.CakeniDimens
import com.stupidtree.hitax.replica.cakeni.CakeniWeek
import com.stupidtree.hitax.replica.cakeni.layoutCakeniDay
import com.stupidtree.hitax.replica.cakeni.mondayStart
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private const val DAY_MS = 24L * 60 * 60 * 1000
private const val WEEK_MS = 7L * DAY_MS
private const val MIN_SLOT_HEIGHT = 54
private const val MIN_SLOT_COUNT = 11

/** 非本周课程在 showNonCurrentWeek=false 时的透明度，取自 cakeni 的 0.46 观感。 */
private const val INACTIVE_WEEK_ALPHA = 0.4f

private val TimeColumnWidth = 50.dp

private val WEEKDAY_FULL = arrayOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
private val WEEKDAY_SHORT = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

/**
 * cakeni 课表页：大日期头部、周导航、节次时间轴与分栏课程网格。
 *
 * 前置条件：[currentWeek] 是当前查看的周次（不是系统当前周）；[week] 为 null 表示没有课表。
 * 后置条件：week 非空时渲染 week.days 全部天；课程重叠由 [layoutCakeniDay] 分栏。
 * 不变式：系统当前周由 week.week 与 week.weekStart 反推，不依赖外部状态。
 */
@Composable
fun ScheduleScreen(
    week: CakeniWeek?,
    currentWeek: Int,
    weekCount: Int,
    showPeriodTime: Boolean,
    showNonCurrentWeek: Boolean,
    onWeekChange: (Int) -> Unit,
    onCourseClick: (CakeniCourse) -> Unit,
    onAddCourse: () -> Unit,
    onOpenMarket: () -> Unit,
) {
    val now = remember { System.currentTimeMillis() }
    val todayMonday = remember(now) { mondayStart(now) }
    val viewedWeek = (week?.week ?: currentWeek).coerceAtLeast(1)
    val totalWeeks = weekCount.coerceAtLeast(1)
    val realWeek = (if (week == null) {
        viewedWeek
    } else {
        week.week + ((todayMonday - week.weekStart) / WEEK_MS).toInt()
    }).coerceIn(1, totalWeeks)
    val isRealWeek = viewedWeek == realWeek

    // 大日期显示今天；翻到别的周时显示该周周一，日期差推动数字滚动。
    val headerDate = if (isRealWeek) now else (week?.weekStart ?: now)
    val dateText = remember(headerDate) { formatDate(headerDate, "yyyy/MM/dd") }
    val weekdayText = remember(headerDate) { WEEKDAY_FULL[weekdayIndex(headerDate)] }
    val summaryText = remember(week, viewedWeek) {
        val start = week?.weekStart ?: todayMonday
        "第 $viewedWeek 周 · ${formatDate(start, "MM.dd")}-${formatDate(start + 6 * DAY_MS, "MM.dd")} · " +
            "${week?.courseCount ?: 0} 门课"
    }

    // 换周时整块内容从翻页方向滑入；不在首次组合时播放。
    val slide = remember { Animatable(0f) }
    val lastWeek = remember { intArrayOf(viewedWeek) }
    LaunchedEffect(viewedWeek) {
        val previous = lastWeek[0]
        if (viewedWeek != previous) {
            slide.snapTo(if (viewedWeek > previous) 1f else -1f)
            lastWeek[0] = viewedWeek
            slide.animateTo(0f, tween(durationMillis = 260, easing = FastOutSlowInEasing))
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = CakeniDimens.page),
        ) {
            ScheduleHeader(
                dateText = dateText,
                weekdayText = weekdayText,
                isToday = isRealWeek,
                summaryText = summaryText,
                canPrev = viewedWeek > 1,
                canNext = viewedWeek < totalWeeks,
                showBackToCurrent = !isRealWeek,
                onPrev = { if (viewedWeek > 1) onWeekChange(viewedWeek - 1) },
                onNext = { if (viewedWeek < totalWeeks) onWeekChange(viewedWeek + 1) },
                onBackToCurrent = { onWeekChange(realWeek) },
            )
            Spacer(Modifier.height(10.dp))
            if (week == null) {
                NoTimetableState(
                    modifier = Modifier.weight(1f),
                    onAddCourse = onAddCourse,
                    onOpenMarket = onOpenMarket,
                )
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .graphicsLayer {
                            translationX = slide.value * size.width
                            alpha = 1f - abs(slide.value) * 0.55f
                        },
                ) {
                    DayHeaderRow(
                        week = week,
                        todayDayIndex = if (isRealWeek) ((now - todayMonday) / DAY_MS).toInt() else -1,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (week.courseCount == 0) {
                        Text(
                            "本周没有课程，点右下角 + 添加",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    WeekGrid(
                        modifier = Modifier.weight(1f),
                        week = week,
                        showPeriodTime = showPeriodTime,
                        inactiveCourseAlpha = if (showNonCurrentWeek || isRealWeek) 1f else INACTIVE_WEEK_ALPHA,
                        onCourseClick = onCourseClick,
                    )
                }
            }
        }
        if (week != null) {
            FloatingActionButton(
                onClick = onAddCourse,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = CakeniDimens.page, bottom = 16.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "添加课程")
            }
        }
    }
}

@Composable
private fun ScheduleHeader(
    dateText: String,
    weekdayText: String,
    isToday: Boolean,
    summaryText: String,
    canPrev: Boolean,
    canNext: Boolean,
    showBackToCurrent: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onBackToCurrent: () -> Unit,
) {
    val bigStyle = TextStyle(
        fontSize = 42.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum",
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            RollingText(
                text = dateText,
                style = bigStyle,
                lineHeight = 46.dp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(
                Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    weekdayText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isToday) TodayBadge()
            }
        }
        WeekNavButton(Icons.Rounded.ChevronLeft, "上一周", canPrev, onPrev)
        Spacer(Modifier.width(6.dp))
        WeekNavButton(Icons.Rounded.ChevronRight, "下一周", canNext, onNext)
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                summaryText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showBackToCurrent) {
                TextButton(onClick = onBackToCurrent, modifier = Modifier.height(30.dp)) {
                    Text("回到本周", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            } else {
                Text(
                    "本周",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun WeekNavButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        shape = CircleShape,
    ) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(38.dp)) {
            Icon(
                icon,
                contentDescription = description,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
private fun TodayBadge() {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            "今天",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

/**
 * 逐位滚动的文本：只有变化的字符会上下滚动，其余字符原地不动。
 *
 * 前置条件：[text] 与 [style] 使用固定宽度字符（数字开启 tnum），滚动距离取 [lineHeight]。
 * 后置条件：动画结束后每列只剩当前字符。
 */
@Composable
private fun RollingText(
    text: String,
    style: TextStyle,
    lineHeight: Dp,
    color: Color,
    modifier: Modifier = Modifier,
) {
    var previous by remember { mutableStateOf(text) }
    var current by remember { mutableStateOf(text) }
    var forward by remember { mutableStateOf(true) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(text) {
        if (text == current) return@LaunchedEffect
        forward = text >= current
        previous = current
        current = text
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 320, easing = FastOutSlowInEasing))
    }
    val p = progress.value
    val distancePx = with(LocalDensity.current) { lineHeight.toPx() }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        for (index in 0 until maxOf(previous.length, current.length)) {
            val incoming = current.getOrNull(index) ?: continue
            val outgoing = previous.getOrNull(index)?.takeIf { p < 1f && it != incoming }
            Box(
                Modifier
                    .height(lineHeight)
                    .clipToBounds(),
                contentAlignment = Alignment.Center,
            ) {
                if (outgoing == null) {
                    StaticGlyph(incoming, style, color)
                } else {
                    val direction = if (forward) 1f else -1f
                    StaticGlyph(
                        outgoing,
                        style,
                        color,
                        Modifier.graphicsLayer {
                            translationY = -direction * distancePx * p
                            alpha = 1f - p
                        },
                    )
                    StaticGlyph(
                        incoming,
                        style,
                        color,
                        Modifier.graphicsLayer {
                            translationY = direction * distancePx * (1f - p)
                            alpha = p
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StaticGlyph(text: Char, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    Text(
        text.toString(),
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = modifier,
    )
}

@Composable
private fun DayHeaderRow(week: CakeniWeek, todayDayIndex: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(TimeColumnWidth))
        for (day in week.days) {
            val isToday = day.dayIndex == todayDayIndex
            Column(
                Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    WEEKDAY_SHORT.getOrElse(day.dayIndex) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    Modifier
                        .padding(top = 2.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        dayOfMonth(day.dateMillis).toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isToday) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekGrid(
    modifier: Modifier,
    week: CakeniWeek,
    showPeriodTime: Boolean,
    inactiveCourseAlpha: Float,
    onCourseClick: (CakeniCourse) -> Unit,
) {
    val slotCount = remember(week) {
        (week.days.flatMap { it.courses }.maxOfOrNull { it.endSlot } ?: 0)
            .coerceAtLeast(MIN_SLOT_COUNT)
    }
    val labels = remember(week, slotCount) { slotTimeLabels(week, slotCount) }
    val scrollState = rememberScrollState()
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val slotHeight = maxOf(maxHeight / slotCount, MIN_SLOT_HEIGHT.dp)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(slotHeight * slotCount),
            ) {
                TimeColumn(
                    slotCount = slotCount,
                    slotHeight = slotHeight,
                    showPeriodTime = showPeriodTime,
                    startTimes = labels.first,
                    endTimes = labels.second,
                )
                for (day in week.days) {
                    DayColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        day = day,
                        slotCount = slotCount,
                        slotHeight = slotHeight,
                        inactiveCourseAlpha = inactiveCourseAlpha,
                        onCourseClick = onCourseClick,
                    )
                }
            }
            Spacer(Modifier.height(76.dp))
        }
    }
}

@Composable
private fun TimeColumn(
    slotCount: Int,
    slotHeight: Dp,
    showPeriodTime: Boolean,
    startTimes: List<String?>,
    endTimes: List<String?>,
) {
    val numberColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(Modifier.width(TimeColumnWidth).fillMaxHeight()) {
        for (slot in 1..slotCount) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(slotHeight),
            ) {
                if (!showPeriodTime) {
                    Text(
                        slot.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = numberColor,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 4.dp),
                    )
                } else {
                    Text(
                        slot.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = numberColor,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 2.dp, top = 4.dp),
                    )
                    Column(Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 3.dp)) {
                        Text(
                            startTimes.getOrNull(slot).orEmpty(),
                            fontSize = 8.sp,
                            lineHeight = 10.sp,
                            color = numberColor.copy(alpha = 0.9f),
                            maxLines = 1,
                        )
                        Text(
                            endTimes.getOrNull(slot).orEmpty(),
                            fontSize = 8.sp,
                            lineHeight = 10.sp,
                            color = numberColor.copy(alpha = 0.55f),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayColumn(
    modifier: Modifier,
    day: CakeniDay,
    slotCount: Int,
    slotHeight: Dp,
    inactiveCourseAlpha: Float,
    onCourseClick: (CakeniCourse) -> Unit,
) {
    val blocks = remember(day.courses) { layoutCakeniDay(day.courses) }
    val gridLine = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    BoxWithConstraints(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val step = slotHeight.toPx()
            val stroke = 0.8.dp.toPx()
            for (line in 1 until slotCount) {
                val y = step * line
                drawLine(gridLine, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
            }
            drawLine(
                gridLine,
                Offset(0f, 0f),
                Offset(0f, size.height),
                strokeWidth = stroke,
            )
        }
        for (block in blocks) {
            val laneWidth = maxWidth / block.laneCount
            Box(
                Modifier
                    .offset(x = laneWidth * block.lane, y = slotHeight * (block.startSlot - 1))
                    .width(laneWidth)
                    .height(slotHeight * block.span)
                    .padding(horizontal = 1.5.dp, vertical = 2.dp)
                    .graphicsLayer { alpha = inactiveCourseAlpha },
            ) {
                CourseCard(block = block, onClick = onCourseClick)
            }
        }
    }
}

@Composable
private fun CourseCard(block: CakeniBlock, onClick: (CakeniCourse) -> Unit) {
    val course = block.course
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accent = remember(course.id, course.color) {
        if (course.color != 0) Color(course.color) else CakeniCourseColors.colorFor(course.id)
    }
    val surface = MaterialTheme.colorScheme.surface
    val container = accent
        .copy(alpha = if (dark) 0.30f else 0.16f)
        .compositeOver(surface)
    val textColor = MaterialTheme.colorScheme.onSurface
    val subColor = MaterialTheme.colorScheme.onSurfaceVariant
    val timeLabel = remember(course.from, course.to) {
        "${formatDate(course.from, "HH:mm")}-${formatDate(course.to, "HH:mm")}"
    }
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(CakeniDimens.courseRadius))
            .background(container)
            .clickable { onClick(course) },
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(3.5.dp)
                .background(accent),
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 3.dp),
        ) {
            Text(
                displayCourseName(course.name),
                fontSize = 10.5.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (course.place.isNotBlank()) {
                Text(
                    course.place,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    color = subColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            if (block.span >= 2 && course.teacher.isNotBlank()) {
                Text(
                    course.teacher,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    color = subColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (block.span >= 3) {
                Text(
                    timeLabel,
                    fontSize = 8.sp,
                    lineHeight = 10.sp,
                    color = subColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun NoTimetableState(
    modifier: Modifier,
    onAddCourse: () -> Unit,
    onOpenMarket: () -> Unit,
) {
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
        )
        Text(
            "还没有课表",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            "导入教务课表或先添加一门课程",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onAddCourse) { Text("添加课程") }
            TextButton(onClick = onOpenMarket) { Text("插件市场") }
        }
    }
}

private fun displayCourseName(value: String): String {
    val cleaned = value
        .replaceFirst(Regex("^\\s*\\d{6,}\\s*-\\s*"), "")
        .replace(Regex("\\s*\\[[^]]+]\\s*$"), "")
        .trim()
    return cleaned.ifBlank { value }
}

/**
 * 从已加载课程反推每个节次的起止时间；某个节次没有课程时该槽位为 null。
 */
private fun slotTimeLabels(week: CakeniWeek, slotCount: Int): Pair<List<String?>, List<String?>> {
    val starts = MutableList<String?>(slotCount + 1) { null }
    val ends = MutableList<String?>(slotCount + 1) { null }
    week.days.flatMap { it.courses }.forEach { course ->
        val start = course.startSlot.coerceIn(1, slotCount)
        val end = course.endSlot.coerceIn(start, slotCount)
        if (starts[start] == null) starts[start] = formatDate(course.from, "HH:mm")
        if (ends[end] == null) ends[end] = formatDate(course.to, "HH:mm")
    }
    return starts to ends
}

private fun formatDate(timestamp: Long, pattern: String): String =
    SimpleDateFormat(pattern, Locale.CHINA).format(Date(timestamp))

private fun weekdayIndex(timestamp: Long): Int {
    val dayOfWeek = Calendar.getInstance().apply { timeInMillis = timestamp }
        .get(Calendar.DAY_OF_WEEK)
    return (dayOfWeek + 5) % 7
}

private fun dayOfMonth(timestamp: Long): Int =
    Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_MONTH)
