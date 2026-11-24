package com.stupidtree.hitax.replica.cakeni

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import androidx.lifecycle.MutableLiveData
import com.stupidtree.hita.api.HostUiService
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hita.api.TodoService
import com.stupidtree.hitax.data.model.timetable.Timetable
import com.stupidtree.hitax.replica.cakeni.importdata.ImportScreen
import com.stupidtree.hitax.replica.cakeni.schedule.CourseDetailSheet
import com.stupidtree.hitax.replica.cakeni.schedule.ScheduleScreen
import com.stupidtree.hitax.replica.cakeni.settings.SettingsScreen
import com.stupidtree.hitax.replica.cakeni.todo.TodoScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class CakeniTab(val title: String, val icon: ImageVector) {
    Schedule("课表", Icons.Outlined.CalendarMonth),
    Todo("待办", Icons.Outlined.Checklist),
    Import("导入", Icons.Outlined.UploadFile),
    Settings("设置", Icons.Outlined.Settings),
}

/**
 * cakeni 风格完整 Shell。
 *
 * 前置条件：插件服务已装载；timetable/todo/hostUi 均可为 null。
 * 后置条件：4 个 tab 可切换；设置只影响本 Shell 的展示状态。
 */
@Composable
fun CakeniReplicaRoot(
    timetableService: TimetableService?,
    todoService: TodoService?,
    hostUi: HostUiService?,
    onOpenMarket: () -> Unit,
    onImport: () -> Unit,
    onAddCourse: (Timetable?) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var darkTheme by rememberSaveable { mutableStateOf(CakeniPrefs.dark(context)) }
    val systemDark = isSystemInDarkTheme()
    val isDark = darkTheme ?: systemDark
    CakeniReplicaTheme(darkTheme = isDark) {
        val live = remember(timetableService) { timetableService?.getRecentTimetable() }
        val empty = remember { MutableLiveData<Timetable?>(null) }
        val timetable by (live ?: empty).observeAsState(initial = null)
        var tab by rememberSaveable { mutableStateOf(CakeniTab.Schedule) }
        var week by rememberSaveable { mutableIntStateOf(1) }
        var showWeekend by rememberSaveable { mutableStateOf(CakeniPrefs.showWeekend(context)) }
        var showNonCurrentWeek by rememberSaveable { mutableStateOf(CakeniPrefs.showNonCurrent(context)) }
        var showPeriodTime by rememberSaveable { mutableStateOf(CakeniPrefs.showPeriodTime(context)) }
        var selected by remember { mutableStateOf<CakeniCourse?>(null) }
        val dayCount = if (showWeekend) 7 else 5

        LaunchedEffect(timetable?.id) { timetable?.let { week = currentWeek(it) } }
        val weekData by produceState<CakeniWeek?>(null, timetable?.id, week, dayCount, timetableService) {
            val current = timetable
            value = if (current == null) null else withContext(Dispatchers.IO) {
                CakeniSource.loadWeek(timetableService, current, week, dayCount)
            }
        }

        BackHandler(enabled = selected != null) { selected = null }
        BackHandler(enabled = selected == null && tab != CakeniTab.Schedule) {
            tab = CakeniTab.Schedule
        }

        val hazeState = remember { HazeState() }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().statusBarsPadding()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .haze(hazeState)
                        .padding(bottom = 86.dp),
                ) {
                    when (tab) {
                        CakeniTab.Schedule -> ScheduleScreen(
                            week = weekData,
                            currentWeek = week,
                            weekCount = timetable?.let(::weekCount) ?: 1,
                            showPeriodTime = showPeriodTime,
                            showNonCurrentWeek = showNonCurrentWeek,
                            onWeekChange = { week = it },
                            onCourseClick = { selected = it },
                            onAddCourse = { onAddCourse(timetable) },
                            onOpenMarket = onOpenMarket,
                        )
                        CakeniTab.Todo -> TodoScreen(todoService, onOpenMarket)
                        CakeniTab.Import -> ImportScreen(onImport = onImport, onOpenMarket = onOpenMarket)
                        CakeniTab.Settings -> SettingsScreen(
                            darkTheme = darkTheme,
                            onDarkThemeChange = {
                                darkTheme = it
                                CakeniPrefs.setDark(context, it)
                            },
                            showWeekend = showWeekend,
                            onShowWeekendChange = {
                                showWeekend = it
                                CakeniPrefs.setShowWeekend(context, it)
                            },
                            showNonCurrentWeek = showNonCurrentWeek,
                            onShowNonCurrentWeekChange = {
                                showNonCurrentWeek = it
                                CakeniPrefs.setShowNonCurrent(context, it)
                            },
                            showPeriodTime = showPeriodTime,
                            onShowPeriodTimeChange = {
                                showPeriodTime = it
                                CakeniPrefs.setShowPeriodTime(context, it)
                            },
                            onOpenMarket = onOpenMarket,
                        )
                    }
                }
                CakeniBottomBar(
                    current = tab,
                    onSelect = { tab = it },
                    hazeState = hazeState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
        selected?.let { course ->
            CourseDetailSheet(
                course = course,
                week = weekData?.week ?: week,
                onDismiss = { selected = null },
            )
        }
    }
}

@Composable
private fun CakeniBottomBar(
    current: CakeniTab,
    onSelect: (CakeniTab) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    Surface(
        // API 31+ 由 haze 提供背景模糊；低版本保留半透明+边框的降级。
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        tonalElevation = 0.dp,
        shadowElevation = 10.dp,
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .hazeChild(
                state = hazeState,
                shape = shape,
                style = HazeStyle(
                    tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                    blurRadius = 26.dp,
                    noiseFactor = 0.04f,
                ),
            ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (item in CakeniTab.entries) {
                val active = item == current
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelect(item) }
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp).size(22.dp),
                            tint = if (active) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        item.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
