package com.stupidtree.hitax.shell

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import android.view.HapticFeedbackConstants
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.text.style.TextOverflow
import com.stupidtree.hita.api.FeedbackEvent
import com.stupidtree.hita.api.FeedbackService
import com.stupidtree.hita.api.MotionController
import com.stupidtree.hita.api.MotionEasing
import com.stupidtree.hita.api.MotionTokens
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.campus.CampusCatalog
import com.stupidtree.hitax.R
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.shell.homework.HomeworkRepository
import com.stupidtree.hitax.shell.homework.HomeworkScreen
import com.stupidtree.hitax.agent.AgentChatViewModel
import com.stupidtree.hitax.shell.agent.AgentChatScreen
import com.stupidtree.hitax.shell.homework.HomeworkViewModel
import com.stupidtree.hitax.shell.news.ShellNewsPage
import com.stupidtree.hitax.shell.style.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 独立 Preset Shell 入口：不经过 MainActivity 的固定四页壳。
 * 当前里程碑：五 tab 导航 + 真实今日课程页（数据来自 TimetableService）。
 */
class PresetShellActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_SHELL_ID = "shellId"
    }

    private var redirectedBack = false

    override fun onResume() {
        super.onResume()
        if (redirectedBack || isFinishing) return
        val shell = com.stupidtree.hitax.market.PresetStateStore.activeShell(this)
        val current = intent.getStringExtra(EXTRA_SHELL_ID)
        if (shell.isNullOrBlank() || (current != null && shell != current)) {
            redirectedBack = true
            startActivity(
                android.content.Intent(this, com.stupidtree.hitax.ui.main.MainActivity::class.java)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val service = (application as HApplication).pluginHost.service(TimetableService.KEY)
        val homeworkViewModel = HomeworkViewModel(HomeworkRepository(application))
        val agentViewModel = AgentChatViewModel(application)
        setContent {
            val dark = isSystemInDarkTheme()
            val style = (application as HApplication).pluginHost
                .service(com.stupidtree.hita.api.ThemeController.KEY)?.themeStyle() ?: "classic"
            val hazeState = remember { dev.chrisbanes.haze.HazeState() }
            val scheme = shellStyleScheme(style, dark)
            CompositionLocalProvider(LocalShellStyle provides ShellStyleState(style, dark)) {
                MaterialTheme(
                    colorScheme = scheme,
                    typography = shellStyleTypography(style),
                    shapes = shellStyleShapes(style),
                ) {
                    Box(Modifier.fillMaxSize().background(scheme.background)) {
                        Box(
                            Modifier.fillMaxSize().then(
                                if (style == "apple_glass") {
                                    Modifier.haze(hazeState)
                                } else Modifier
                            )
                        ) { ShellBackdrop(style, dark) }
                        HitaAndroidShell(service, homeworkViewModel, agentViewModel, style, hazeState)
                    }
                }
            }
        }
    }
}

@Composable
private fun shellSecondary(): Color =
    if (isSystemInDarkTheme()) Color(0xFFC2C8D2) else Color(0xFF6B7280)

@Composable
private fun shellTertiary(): Color =
    if (isSystemInDarkTheme()) Color(0xFFAEB6C2) else Color(0xFF9CA3AF)

private fun MotionEasing.toComposeEasing(): Easing = when (this) {
    MotionEasing.EMPHASIZED -> LinearOutSlowInEasing
    MotionEasing.LINEAR -> LinearEasing
    else -> FastOutSlowInEasing
}

private enum class ShellTarget {
    TODO,
    TODAY,
    TIMETABLE,
    ASSISTANT,
    NEWS,
    MORE,
    PLUGIN,
    UNAVAILABLE,
}

private data class ShellTab(
    val id: String,
    val title: String,
    val iconRes: Int,
    val target: ShellTarget,
    val pluginTab: com.stupidtree.hita.api.MainTabSpec? = null,
    /** 提供该 tab 的 UI 插件 id；多 tab 插件不能拿 tab id 当插件 id。 */
    val pluginId: String? = null,
)

private fun defaultShellTabs(): List<ShellTab> = listOf(
    ShellTab("com.stupidtree.hita.ui.homework", "待办", R.drawable.ic_baseline_check_24, ShellTarget.TODO),
    ShellTab("hita.timeline", "今日", R.drawable.ic_nav_today, ShellTarget.TODAY),
    ShellTab("hita.timetable", "时间表", R.drawable.ic_nav_timetable, ShellTarget.TIMETABLE),
    ShellTab("com.stupidtree.hita.ui.ai", "助手", R.drawable.ic_baseline_toys_24, ShellTarget.ASSISTANT),
    ShellTab("com.stupidtree.hita.ui.news", "资讯", R.drawable.ic_nav_blog, ShellTarget.NEWS),
    ShellTab("hita.more", "更多", R.drawable.ic_nav_navigation, ShellTarget.MORE),
)

/** 读取当前整合包/本地布局；无数据时回退到 0.7.3 的固定六页。 */
private fun loadShellTabs(context: android.content.Context): List<ShellTab> {
    val stored = com.stupidtree.hitax.market.PresetLayoutStore.tabs(context)
    if (stored.isEmpty()) return defaultShellTabs()
    val effective = stored.filter {
        it.visible != false || it.required == true || it.id == "hita.more"
    }.ifEmpty { stored }
    if (effective.isEmpty()) return defaultShellTabs()
    val pluginTabs = runCatching {
        com.stupidtree.hitax.uiplugin.UiPluginManager(context).mainTabs()
    }.getOrDefault(emptyList())
    return effective.sortedBy { it.order }.map { spec ->
        val pluginTab = pluginTabs.firstOrNull {
            it.plugin.id == spec.id || it.spec.id == spec.id
        }
        val target = when (spec.id) {
            "com.stupidtree.hita.ui.homework" -> ShellTarget.TODO
            "hita.timeline" -> ShellTarget.TODAY
            "hita.timetable", "com.stupidtree.hita.ui.timetable" -> ShellTarget.TIMETABLE
            "com.stupidtree.hita.ui.ai" -> ShellTarget.ASSISTANT
            "com.stupidtree.hita.ui.news" -> ShellTarget.NEWS
            "hita.more" -> ShellTarget.MORE
            else -> if (pluginTab != null) ShellTarget.PLUGIN else ShellTarget.UNAVAILABLE
        }
        val fallbackIcon = when (target) {
            ShellTarget.TODO -> R.drawable.ic_baseline_check_24
            ShellTarget.TODAY -> R.drawable.ic_nav_today
            ShellTarget.TIMETABLE -> R.drawable.ic_nav_timetable
            ShellTarget.ASSISTANT -> R.drawable.ic_baseline_toys_24
            ShellTarget.NEWS -> R.drawable.ic_nav_blog
            ShellTarget.MORE -> R.drawable.ic_nav_navigation
            ShellTarget.PLUGIN -> R.drawable.ic_nav_plugin
            ShellTarget.UNAVAILABLE -> R.drawable.ic_nav_plugin
        }
        ShellTab(
            id = spec.id,
            title = spec.title?.takeIf { it.isNotBlank() } ?: spec.id,
            iconRes = com.stupidtree.hitax.market.BuiltinTabIcons.resolve(spec.iconAsset)
                ?: fallbackIcon,
            target = target,
            pluginTab = pluginTab?.spec,
            pluginId = pluginTab?.plugin?.id,
        )
    }
}

@Composable
private fun HitaAndroidShell(
    service: TimetableService?,
    homeworkViewModel: HomeworkViewModel,
    agentViewModel: AgentChatViewModel,
    style: String,
    hazeState: dev.chrisbanes.haze.HazeState?,
) {
    val shellContext = androidx.compose.ui.platform.LocalContext.current
    val tabs = remember(shellContext) { loadShellTabs(shellContext) }
    var selectedId by remember(tabs) {
        mutableStateOf(tabs.firstOrNull { it.id == "hita.timeline" }?.id ?: tabs.firstOrNull()?.id)
    }
    val selected = tabs.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 } ?: 0
    var drawerOpen by remember { mutableStateOf(false) }
    val drawerProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (drawerOpen) 1f else 0f,
        animationSpec = if (style == "apple_glass") {
            androidx.compose.animation.core.tween(
                durationMillis = ShellMotion.drawerDurationMs(style, drawerOpen),
                easing = if (drawerOpen) {
                    androidx.compose.animation.core.LinearOutSlowInEasing
                } else {
                    androidx.compose.animation.core.FastOutLinearInEasing
                },
            )
        } else {
            androidx.compose.animation.core.spring(dampingRatio = 0.9f, stiffness = 300f)
        },
        label = "drawer_progress",
    )
    val density = LocalDensity.current
    val drawerWidthPx = with(density) { 260.dp.toPx() }
    val hostApplication = shellContext.applicationContext as? HApplication
    val motion = remember(hostApplication) {
        hostApplication?.pluginHost?.service(MotionController.KEY)
    }
    val motionLive = remember(motion) {
        motion?.observe() ?: androidx.lifecycle.MutableLiveData(MotionTokens())
    }
    val motionTokens by motionLive.observeAsState(MotionTokens())
    val feedback = remember(hostApplication) {
        hostApplication?.pluginHost?.service(FeedbackService.KEY)
    }
    val tabEasing = motionTokens.easing.toComposeEasing()
    val tabSpec = tween<Float>(
        durationMillis = if (motionTokens.enabled) motionTokens.durationShortMs else 0,
        easing = tabEasing,
    )
    var showUsageConsent by remember {
        mutableStateOf(!com.stupidtree.hitax.data.source.preference.UsageStatsStore.hasAsked(shellContext))
    }
    if (showUsageConsent) {
        AlertDialog(
            onDismissRequest = {
                com.stupidtree.hitax.data.source.preference.UsageStatsStore.markAsked(shellContext)
                showUsageConsent = false
            },
            title = { Text("匿名使用统计") },
            text = {
                Text(
                    "是否允许上报匿名安装 ID、版本、事件名、页面和插件 ID？" +
                        "不会上报账号、学号、姓名、课程内容或自由文本。默认关闭，可随时在更多页关闭或清除。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    com.stupidtree.hitax.data.source.preference.UsageStatsStore.setEnabled(shellContext, true)
                    com.stupidtree.hitax.analytics.UsageAnalytics.record(
                        shellContext, com.stupidtree.hitax.analytics.UsageEventAllowlist.APP_OPEN
                    )
                    showUsageConsent = false
                }) { Text("允许匿名统计") }
            },
            dismissButton = {
                TextButton(onClick = {
                    com.stupidtree.hitax.data.source.preference.UsageStatsStore.markAsked(shellContext)
                    showUsageConsent = false
                }) { Text("暂不") }
            },
        )
    }
    val screenNames = mapOf(
        ShellTarget.TODO to "todo",
        ShellTarget.TODAY to "today",
        ShellTarget.TIMETABLE to "timetable",
        ShellTarget.ASSISTANT to "assistant",
        ShellTarget.NEWS to "news",
        ShellTarget.MORE to "more",
    )
    LaunchedEffect(selected, tabs) {
        screenNames[tabs.getOrNull(selected)?.target]?.let { screen ->
            com.stupidtree.hitax.analytics.UsageAnalytics.record(
                shellContext,
                com.stupidtree.hitax.analytics.UsageEventAllowlist.SCREEN_VIEW,
                screen = screen,
            )
        }
    }
    BackHandler(enabled = drawerOpen) { drawerOpen = false }
    val tabSwipe = rememberTabSwipe(
        enabled = tabs.getOrNull(selected)?.target != ShellTarget.TIMETABLE,
        currentIndex = selected,
        pageCount = tabs.size,
        onSelect = { index ->
            tabs.getOrNull(index)?.let { tab ->
                selectedId = tab.id
                feedback?.play(FeedbackEvent.TAB)
            }
        },
    )
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxSize().graphicsLayer {
            val scale = ShellMotion.drawerScale(style, drawerProgress)
            scaleX = scale
            scaleY = scale
            translationX = ShellMotion.drawerOffsetPx(
                style, drawerProgress, drawerWidthPx, density.density,
            )
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f)
        },
    ) {
        Box(
            Modifier.weight(1f).then(tabSwipe.modifier).graphicsLayer {
                translationX = tabSwipe.offset.value
                alpha = tabSwipe.alpha.value
            },
        ) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(tabSpec) togetherWith fadeOut(tabSpec) },
                label = "shell_tab",
            ) { index ->
                val tab = tabs.getOrNull(index)
                when (tab?.target) {
                    ShellTarget.TODO -> HomeworkScreen(homeworkViewModel)
                    ShellTarget.TODAY -> TodayPage(service, motionTokens, style, hazeState)
                    ShellTarget.TIMETABLE -> TimetablePage(service, motionTokens)
                    ShellTarget.ASSISTANT -> AgentChatScreen(
                        agentViewModel,
                        remember(service) {
                            runCatching { service?.getRecentTimetable()?.value?.id }.getOrNull()
                        },
                    )
                    ShellTarget.NEWS -> ShellNewsPage()
                    ShellTarget.MORE -> MorePage(
                        service,
                        onOpenDrawer = { drawerOpen = true },
                    )
                    ShellTarget.PLUGIN -> tab.pluginTab?.let {
                        RuntimeUiTabPage(tab.pluginId ?: tab.id, it, tab.title)
                    } ?: UnavailableShellPage(tab.title)
                    ShellTarget.UNAVAILABLE -> UnavailableShellPage(tab.title)
                    null -> Unit
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(bottom = 0.dp), contentAlignment = Alignment.BottomCenter) {
            val availableWidth = LocalConfiguration.current.screenWidthDp.dp - 32.dp
            val tabWidth = minOf(60.dp, availableWidth / tabs.size.coerceAtLeast(1))
            val barWidth = tabWidth * tabs.size + 8.dp
            val view = LocalView.current
            val density = LocalDensity.current
            val tabWidthPx = with(density) { tabWidth.toPx() }
            val barWidthPx = with(density) { barWidth.toPx() }
            val paddingPx = with(density) { 4.dp.toPx() }
            val dock = rememberShellDockState(
                selected = selected,
                onSelect = { index ->
                    tabs.getOrNull(index)?.let { tab ->
                        selectedId = tab.id
                        feedback?.play(FeedbackEvent.TAB)
                    }
                },
                onTick = { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) },
            )
            val lensExtra = tabWidth * (dock.stretch.value * 0.5f)
            val lensHeight = 44.dp * (1f + dock.lift.value * 0.12f)
            val indicatorOffset = tabWidth * dock.lens.value - lensExtra / 2f
            val selectedNow = androidx.compose.runtime.rememberUpdatedState(selected)
            val glassDockBase = if (isSystemInDarkTheme()) Color(0xFF121923) else Color.White
            val dockShape = hitaStyleCardShape(28.dp, 16.dp)
            val dockBase = when {
                style == "apple_glass" -> glassDockBase.copy(alpha = 0.95f)
                style == "sora_cloud" -> if (isSystemInDarkTheme()) Color(0xFF25221D) else SoraPaper
                else -> MaterialTheme.colorScheme.surface
            }
            Surface(
                shape = dockShape,
                color = if (style == "apple_glass") dockBase else hitaGlassContainerColor(dockBase),
                border = hitaGlassCardBorder(),
                shadowElevation = 6.dp,
                modifier = Modifier.width(barWidth).height(54.dp)
                    .hitaGlassCardModifier(dockShape, 8.dp)
                    .graphicsLayer {
                        val s = ShellMotion.pressScale(
                            dock.swell.value,
                            size.maxDimension,
                            with(density) { 16.dp.toPx() },
                        )
                        scaleX = s
                        scaleY = s
                    }
                    .pointerInput(tabs.size, barWidthPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            dock.press(down.position.x, barWidthPx, tabs.size, paddingPx)
                            val tracker = VelocityTracker()
                            tracker.addPosition(down.uptimeMillis, down.position)
                            var dragged = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.pressed) {
                                    tracker.addPosition(change.uptimeMillis, change.position)
                                    if (kotlin.math.abs(change.position.x - down.position.x) > viewConfiguration.touchSlop) {
                                        dragged = true
                                    }
                                    if (dragged) {
                                        dock.drag(
                                            change.position.x,
                                            barWidthPx,
                                            tabs.size,
                                            paddingPx,
                                            viewConfiguration.touchSlop,
                                            size.height.toFloat(),
                                        )
                                    }
                                    change.consume()
                                } else {
                                    dock.release(
                                        tracker.calculateVelocity().x,
                                        barWidthPx,
                                        tabs.size,
                                        paddingPx,
                                        selectedNow.value,
                                    )
                                    break
                                }
                            }
                        }
                    }
                    .then(
                    if (style == "apple_glass" && hazeState != null) {
                        Modifier.hazeChild(
                            state = hazeState,
                            shape = RoundedCornerShape(28.dp),
                            style = HazeStyle(
                                tint = Color.White.copy(alpha = 0.16f),
                                blurRadius = 24.dp,
                                noiseFactor = 0.05f,
                            ),
                        )
                            .border(0.8.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(28.dp))
                    } else Modifier
                ),
            ) {
                Box(Modifier.fillMaxSize().padding(4.dp)) {
                    Surface(
                        modifier = Modifier
                            .offset(x = indicatorOffset, y = -(lensHeight - 44.dp) / 2f)
                            .width(tabWidth + lensExtra)
                            .height(lensHeight),
                        shape = RoundedCornerShape(22.dp),
                        color = when (style) {
                            "p5" -> MaterialTheme.colorScheme.primary
                            "sora_cloud" -> Color(0xFFC94B38)
                            "sumi" -> MaterialTheme.colorScheme.secondary
                            "cyber", "apple_glass", "deep_space" ->
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else -> Color(0xFF3390EC).copy(alpha = 0.12f)
                        },
                    ) {}
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        val dockStyle = style in setOf("classic", "apple_glass", "deep_space")
                        val iconSize = if (dockStyle) 20.dp else 18.dp
                        tabs.forEachIndexed { index, tab ->
                            val active = selected == index
                            val tint = if (active) {
                                when (style) {
                                    "apple_glass", "deep_space" -> MaterialTheme.colorScheme.primary
                                    "p5" -> MaterialTheme.colorScheme.onPrimary
                                    "sora_cloud" -> SoraPaper
                                    "sumi" -> Color(0xFFF7F2E7)
                                    else -> Color(0xFF0088CC)
                                }
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                            Column(
                                Modifier
                                    .width(tabWidth)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(22.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    painter = painterResource(tab.iconRes),
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(iconSize),
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    tab.title,
                                    color = tint,
                                    fontSize = 10.sp,
                                    lineHeight = 10.sp,
                                    letterSpacing = 0.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    ShellDrawerOverlay(drawerOpen, drawerProgress, style, hazeState) { drawerOpen = false }
    }
}

@Composable
private fun ShellDrawerOverlay(
    open: Boolean,
    drawerProgress: Float,
    style: String,
    hazeState: dev.chrisbanes.haze.HazeState?,
    onClose: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val eas = remember {
        (context.applicationContext as? HApplication)?.pluginHost?.service(
            com.stupidtree.hita.api.EasService.KEY,
        )
    }
    val userName = eas?.accountName()
    val accountPrefs = remember {
        context.getSharedPreferences("local_eas_token", android.content.Context.MODE_PRIVATE)
    }
    val userSubtitle = run {
        val id = eas?.accountSubtitle()?.takeIf { it.isNotBlank() }
            ?: accountPrefs.getString("stuId", null)?.takeIf { it.isNotBlank() }
        val grade = accountPrefs.getString("grade", null)?.takeIf(String::isNotBlank)
            ?: id?.take(4)?.takeIf { it.length == 4 }?.plus("级")
        val details = listOf(
            accountPrefs.getString("school", null),
            accountPrefs.getString("major", null),
            grade,
        ).filterNotNull().filter(String::isNotBlank).joinToString(" · ")
        listOfNotNull(id, details.takeIf { it.isNotBlank() })
            .joinToString("\n")
            .takeIf { it.isNotBlank() }
    }
    val theme = remember {
        (context.applicationContext as? HApplication)?.pluginHost?.service(
            com.stupidtree.hita.api.ThemeController.KEY,
        )
    }
    val styleLabel = when (theme?.themeStyle()) {
        "apple_glass" -> "玻璃艺术"
        "cyber" -> "赛博朋克"
        "sora_cloud" -> "日映构成"
        "p5" -> "波普涂鸦"
        "deep_space" -> "深空星野"
        "sumi" -> "墨韵"
        else -> "经典搭配"
    }
    fun launch(className: String) {
        runCatching {
            context.startActivity(
                android.content.Intent().setClassName(context, className)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        onClose()
    }
    var showStyleMenu by remember { mutableStateOf(false) }
    val styleId = theme?.themeStyle() ?: "classic"
    val styleLabels = listOf(
        "classic" to "经典搭配",
        "apple_glass" to "玻璃艺术",
        "cyber" to "赛博朋克",
        "sora_cloud" to "日映构成",
        "p5" to "波普涂鸦",
        "deep_space" to "深空星野",
        "sumi" to "墨韵",
    )
    val drawerWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { 260.dp.toPx() }
    if (showStyleMenu) {
        AlertDialog(
            onDismissRequest = { showStyleMenu = false },
            title = { Text("界面风格") },
            text = {
                Column {
                    styleLabels.forEach { (id, label) ->
                        Text(
                            (if (id == styleId) "✓ " else "   ") + label,
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    if (theme?.setThemeStyle(id) == true) {
                                        showStyleMenu = false
                                        onClose()
                                        (context as? android.app.Activity)?.recreate()
                                    }
                                }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showStyleMenu = false }) { Text("取消") } },
        )
    }
    if (drawerProgress <= 0.001f) return
    Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(
                    Color.Black.copy(
                        alpha = (if (style == "apple_glass") 0.22f else 0.32f) * drawerProgress,
                    ),
                )
                    .clickable(enabled = drawerProgress > 0.1f) { onClose() },
            )
            val drawerShape = hitaStyleCardShape(0.dp, 18.dp)
            Surface(
                shape = drawerShape,
                border = hitaGlassCardBorder(),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(260.dp)
                    .hitaGlassCardModifier(drawerShape, 18.dp)
                    .graphicsLayer { translationX = (1f - drawerProgress) * drawerWidthPx }
                    .then(
                        if (style == "apple_glass" && hazeState != null) {
                            Modifier.hazeChild(
                                state = hazeState,
                                shape = RoundedCornerShape(0.dp),
                                style = HazeStyle(
                                    tint = Color.White.copy(alpha = 0.60f),
                                    blurRadius = 20.dp,
                                    noiseFactor = 0.03f,
                                ),
                            )
                                .border(0.8.dp, Color.White.copy(alpha = 0.42f), RoundedCornerShape(0.dp))
                        } else Modifier
                    ),
                color = if (style == "apple_glass") {
                    if (isSystemInDarkTheme()) Color(0xFF121923) else Color(0xFFEEF1F6)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shadowElevation = 12.dp,
            ) {
                Column(
                    Modifier.fillMaxSize().statusBarsPadding()
                        .padding(horizontal = 16.dp),
                ) {
                    Column(
                        Modifier.fillMaxWidth()
                            .clickable {
                                val activity = context as? androidx.fragment.app.FragmentActivity
                                if (activity != null && eas != null) {
                                    eas.openAccount(activity)
                                } else {
                                    launch("com.stupidtree.hitax.ui.eas.login.ShenzhenWebLoginActivity")
                                }
                            }
                            .padding(top = 78.dp, bottom = 17.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.place_holder_avatar),
                            contentDescription = null,
                            modifier = Modifier.size(72.dp).clip(CircleShape),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            userName ?: "点击进行教务登录",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            userSubtitle ?: "未登录教务",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(Color(0xFFE5E7EB)),
                    )
                    DrawerRow(
                        title = "界面风格 · $styleLabel",
                        iconRes = R.drawable.ic_baseline_color_lens_24,
                    ) {
                        showStyleMenu = true
                    }
                    DrawerRow(
                        title = "用户协议与隐私政策",
                        iconRes = R.drawable.ic_info,
                    ) {
                        (context as? androidx.fragment.app.FragmentActivity)?.let {
                            com.stupidtree.hitax.ui.about.UserAgreementDialog()
                                .show(it.supportFragmentManager, "ua")
                        }
                        onClose()
                    }
                    DrawerRow(
                        title = "用户反馈",
                        iconRes = R.drawable.ic_baseline_edit_24,
                    ) {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://github.com/kajimi16/OH-MY-HITA/issues"),
                            )
                        )
                        onClose()
                    }
                    DrawerRow(
                        title = "关于HITA",
                        iconRes = R.drawable.logo,
                    ) {
                        context.startActivity(
                            android.content.Intent(context, com.stupidtree.hitax.ui.about.ActivityAbout::class.java)
                        )
                        onClose()
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(Color(0xFFE5E7EB)),
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Co-authored by",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "茶蘼 | 离谱 | 北极熊 | 软糯",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "纪行 | RadiantAbyss",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Limpu 的赞赏码",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(7.dp))
                }
            }
        }
    }

@Composable
private fun DrawerRow(
    title: String,
    iconRes: Int? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            icon != null -> androidx.compose.material3.Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            iconRes != null -> androidx.compose.material3.Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun UnavailableShellPage(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "$title 页面不可用",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp,
        )
    }
}

@Composable
private fun TodayPage(
    service: TimetableService?,
    motionTokens: MotionTokens,
    style: String,
    hazeState: dev.chrisbanes.haze.HazeState?,
) {
    val dateText = remember { SimpleDateFormat("M月d日 EEE", Locale.CHINA).format(Date()) }
    var events by remember(service) {
        mutableStateOf<List<com.stupidtree.hitax.data.model.timetable.EventItem>>(emptyList())
    }
    LaunchedEffect(service) {
        events = withContext(Dispatchers.IO) {
            runCatching { service?.getTodayEventsSync().orEmpty() }.getOrDefault(emptyList())
        }
    }
    val finished = events.isNotEmpty() && events.all { it.to.time <= System.currentTimeMillis() }
    var expanded by remember { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val nextEvent = events.filter { it.to.time > now }.minByOrNull { it.from.time }
    val countdown = nextEvent?.let {
        val minutes = ((it.from.time - now) / 60_000L).coerceAtLeast(0L)
        when {
            it.from.time <= now -> "正在进行"
            minutes >= 60 -> "${minutes / 60}小时${minutes % 60}分钟后进行"
            else -> "${minutes}分钟后进行"
        }
    }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween<Float>(
            durationMillis = if (motionTokens.enabled) motionTokens.durationShortMs else 0,
            easing = motionTokens.easing.toComposeEasing(),
        ),
        label = "todayArrow",
    )
    Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 20.4.dp)) {
        Text(
            dateText,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MaterialTheme.typography.displayLarge.fontFamily,
        )
        Spacer(Modifier.height(39.dp))
        val style = (androidx.compose.ui.platform.LocalContext.current.applicationContext as? HApplication)
            ?.pluginHost?.service(com.stupidtree.hita.api.ThemeController.KEY)?.themeStyle() ?: "classic"
        val cardGradient = when (style) {
            "apple_glass" -> listOf(Color(0xFF3F8BCF), Color(0xFF408DCE))
            "cyber" -> listOf(Color(0xFF0C4457), Color(0xFF04050C))
            "sora_cloud" -> listOf(Color(0xFFC94B38), Color(0xFF7C2A20))
            "p5" -> listOf(Color(0xFFE60012), Color(0xFF101010))
            "deep_space" -> listOf(Color(0xFF1A2340), Color(0xFF0B1020))
            "sumi" -> listOf(Color(0xFF33322F), Color(0xFF1C1A16))
            else -> listOf(Color(0xFF1C5E9D), Color(0xFF0C5C91))
        }
        val glass = style == "apple_glass"
        val styled = style in setOf("cyber", "sora_cloud", "p5", "deep_space", "sumi")
        val cardShape = hitaStyleCardShape(16.dp, 14.dp)
        val cardText = if (styled) MaterialTheme.colorScheme.onSurface else Color.White
        val cardTextMuted = if (styled) MaterialTheme.colorScheme.onSurfaceVariant else Color.White.copy(alpha = 0.8f)
        val cardIconBack = if (styled) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = 0.16f)
        val cardPanelBack = if (styled) MaterialTheme.colorScheme.surfaceVariant else Color.White.copy(alpha = 0.16f)
        Card(
            shape = cardShape,
            colors = hitaGlassCardColors(
                containerColor = when {
                    glass -> Color.Transparent
                    styled -> MaterialTheme.colorScheme.surface
                    else -> cardGradient.first()
                },
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = hitaGlassCardBorder(),
            modifier = Modifier.fillMaxWidth()
                .hitaGlassCardModifier(cardShape, 14.dp)
                .clickable { expanded = !expanded },
        ) {
            Column(
                Modifier.fillMaxWidth().then(
                    if (styled) Modifier else Modifier.background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(cardGradient),
                    )
                ),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        when {
                            events.isEmpty() -> "今日空闲"
                            finished -> "全部结束"
                            else -> "今日课程"
                        },
                        color = cardText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        when {
                            events.isEmpty() -> "享受当下！"
                            finished -> "享受生活吧！"
                            else -> "继续加油！"
                        },
                        color = cardTextMuted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Box(
                    Modifier.fillMaxWidth().height(110.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(110.dp).clip(CircleShape)
                            .background(cardIconBack)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(if (finished) R.drawable.ic_finish else R.drawable.ic_timeline_head_free),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.ic_baseline_arrow_drop_down_24),
                    contentDescription = if (expanded) "收起今日课程" else "展开今日课程",
                    tint = cardText,
                    modifier = Modifier.align(Alignment.End).padding(end = 16.dp).size(44.dp)
                        .rotate(arrowRotation),
                )
                val panelDuration = if (motionTokens.enabled) motionTokens.durationMediumMs else 0
                val panelEasing = motionTokens.easing.toComposeEasing()
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(
                        animationSpec = tween<IntSize>(durationMillis = panelDuration, easing = panelEasing),
                    ) + fadeIn(tween<Float>(durationMillis = panelDuration, easing = panelEasing)),
                    exit = shrinkVertically(
                        animationSpec = tween<IntSize>(durationMillis = panelDuration, easing = panelEasing),
                    ) + fadeOut(tween<Float>(durationMillis = panelDuration, easing = panelEasing)),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(cardPanelBack)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                countdown ?: "接下来没事了",
                                color = cardText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                nextEvent?.name ?: "see you",
                                color = cardTextMuted,
                                fontSize = 14.sp,
                            )
                        }
                        androidx.compose.material3.Icon(
                            painter = painterResource(
                                if (nextEvent != null) R.drawable.ic_baseline_access_alarm_24 else R.drawable.ic_empty,
                            ),
                            contentDescription = null,
                            tint = cardText,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }
        if (events.isNotEmpty()) {
            val context = LocalContext.current
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp)) {
                events.sortedBy { it.from.time }.forEach { event ->
                    TodayEventRow(event) {
                        val activity = context as? androidx.fragment.app.FragmentActivity
                        val uiService = (context.applicationContext as? HApplication)
                            ?.pluginHost?.service(com.stupidtree.hita.api.TimetableUiService.KEY)
                        if (activity != null && uiService != null) {
                            uiService.showEventInfo(activity, listOf(event))
                        } else {
                            android.widget.Toast.makeText(context, "该日程没有课程详情", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        if (events.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.fillMaxWidth().heightIn(min = 200.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.Icon(
                        painter = painterResource(R.drawable.ic_timeline),
                        contentDescription = null,
                        tint = Color(0xFF3390EC).copy(alpha = 0.35f),
                        modifier = Modifier.size(96.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("今天暂无课程或日程安排", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun TodayEventRow(event: EventItem, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(32.dp).height(64.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.width(2.dp).fillMaxHeight().background(Color(0xFFE5E7EB)))
            androidx.compose.material3.Icon(
                painter = painterResource(R.drawable.ic_circle_done),
                contentDescription = null,
                tint = shellTertiary(),
                modifier = Modifier.size(22.dp),
            )
        }
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                .clip(RoundedCornerShape(24.dp)),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(event.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TimetablePage(service: TimetableService?, motionTokens: MotionTokens) {
    val timetableLive = remember(service) {
        service?.getRecentTimetable()
            ?: androidx.lifecycle.MutableLiveData<com.stupidtree.hitax.data.model.timetable.Timetable?>(null)
    }
    val timetable by timetableLive.observeAsState(null)
    var weekOffset by remember { mutableIntStateOf(0) }
    val baseWeek = remember(timetable) {
        timetable?.getWeekNumber(System.currentTimeMillis())?.takeIf { it > 0 } ?: 1
    }
    val week = (baseWeek + weekOffset).coerceAtLeast(1)
    val start = remember(timetable, week) {
        (timetable?.startTime?.time ?: currentWeekMondayMillis()) +
            (week - 1).toLong() * 7 * 24 * 3600 * 1000
    }
    val eventsLive = remember(service, start) {
        service?.getEventsDuringWithColor(start, start + 7L * 24 * 3600 * 1000)
            ?: androidx.lifecycle.MutableLiveData(null)
    }
    val eventsState by eventsLive.observeAsState(initial = null)
    val events = eventsState.orEmpty()
    val context = LocalContext.current
    val historyScope = androidx.compose.runtime.rememberCoroutineScope()
    val historyStore = remember(context) { TimetableHistoryStore(context) }
    val historyEnabled = remember(context) {
        com.stupidtree.hitax.market.PresetStateStore.activeId(context)
            ?.startsWith("hita-android-3.3.") == true
    }
    var historyVersions by remember { mutableStateOf<List<TimetableVersionRecord>>(emptyList()) }
    var historyEvents by remember { mutableStateOf<List<EventItem>>(emptyList()) }
    var showHistory by remember { mutableStateOf(false) }
    LaunchedEffect(timetable?.id, eventsState, historyEnabled) {
        val current = timetable
        if (!historyEnabled || current == null || eventsState == null || service == null) return@LaunchedEffect
        val result = withContext(Dispatchers.IO) {
            val all = TimetableHistoryDiff.eventsFor(
                current.id,
                service.getEventsDuringSync(
                    current.startTime.time,
                    current.endTime.time.coerceAtLeast(current.startTime.time + 20L * 7 * 24 * 3600 * 1000),
                ),
            )
            val reason = if (historyStore.versions(current.id).isEmpty()) "INITIAL" else "MANUAL"
            historyStore.record(current.id, all, reason)
            all to historyStore.versions(current.id)
        }
        historyEvents = result.first
        historyVersions = result.second
    }
    val snapshotStore = remember(context) { ShellTimetableSnapshotStore(context) }
    val snapshotKey = "${timetable?.id ?: "none"}:$week"
    var baseline by remember(snapshotKey) { mutableStateOf(snapshotStore.load(snapshotKey)) }
    var pendingSnapshot by remember(snapshotKey) { mutableStateOf<Map<String, String>?>(null) }
    var changeCount by remember(snapshotKey) { mutableIntStateOf(0) }
    LaunchedEffect(snapshotKey, eventsState) {
        val loaded = eventsState ?: return@LaunchedEffect
        val current = ShellTimetableChange.snapshot(loaded)
        val previous = baseline
        if (previous == null) {
            snapshotStore.save(snapshotKey, current)
            baseline = current
        } else if (pendingSnapshot == null) {
            val changed = ShellTimetableChange.diff(previous, current)
            if (changed > 0) {
                changeCount = changed
                pendingSnapshot = current
            } else {
                snapshotStore.save(snapshotKey, current)
                baseline = current
            }
        } else {
            val changed = ShellTimetableChange.diff(previous, current)
            if (changed > 0) {
                changeCount = changed
                pendingSnapshot = current
            } else {
                snapshotStore.save(snapshotKey, current)
                baseline = current
                pendingSnapshot = null
                changeCount = 0
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 13.5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "第${week}周",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                fontFamily = MaterialTheme.typography.displayLarge.fontFamily,
            )
            Spacer(Modifier.width(16.dp))
            Text(
                timetable?.name ?: "还没有课表",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontSize = 12.sp,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
            Spacer(Modifier.weight(1f))
            if (historyEnabled) {
                IconButton(
                    onClick = {
                        showHistory = true
                        timetable?.id?.let { id ->
                            historyScope.launch {
                                historyVersions = withContext(Dispatchers.IO) {
                                    historyStore.versions(id)
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_history_24),
                        contentDescription = "教务版本",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(
                onClick = {
                    val body = buildString {
                        append("第").append(week).append("周 ")
                        append(timetable?.name ?: "默认课表").append("\n")
                        events.sortedBy { it.from.time }.forEach {
                            append(it.from.toString().takeLast(8).take(5)).append(" ").append(it.name).append("\n")
                        }
                    }
                    context.startActivity(
                        android.content.Intent.createChooser(
                            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, body)
                            }, "分享课表"
                        )
                    )
                },
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_share_24),
                    contentDescription = "分享课表",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = {
                    val activity = context as? androidx.fragment.app.FragmentActivity
                    val uiService = (context.applicationContext as? HApplication)
                        ?.pluginHost?.service(com.stupidtree.hita.api.TimetableUiService.KEY)
                    if (activity != null && uiService != null) uiService.openStylePanel(activity)
                },
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_theme),
                    contentDescription = "课表设置",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(Modifier.height(16.4.dp))
        Row(Modifier.fillMaxWidth().padding(top = 0.dp, start = 0.dp, end = 8.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    SimpleDateFormat("M月", Locale.CHINA).format(Date(start)),
                    color = shellTertiary(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { index, day ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    val date = ((start + index * 24L * 3600 * 1000) / 1000L)
                    Box(
                        Modifier.size(26.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(day, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    Text(
                        SimpleDateFormat("d", Locale.CHINA).format(Date(date * 1000L)),
                        color = shellTertiary(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        val hourHeight = 61.dp
        val todayCalendar = java.util.Calendar.getInstance()
        if (todayCalendar.get(java.util.Calendar.HOUR_OF_DAY) < 5) {
            todayCalendar.add(java.util.Calendar.DATE, -1)
        }
        val todayIndex = (dayOfWeek(todayCalendar.timeInMillis) - 1).coerceIn(0, 6)
        val gridScroll = rememberScrollState()
        val hintScope = androidx.compose.runtime.rememberCoroutineScope()
        LaunchedEffect(week) { gridScroll.scrollTo(0) }
        val hasEveningEvents = remember(events) {
            events.any { minuteOfDay(it.from.time) >= 18 * 60 + 30 }
        }
        val hideThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }
        val showEveningHint by remember(hasEveningEvents, gridScroll) {
            derivedStateOf {
                hasEveningEvents && gridScroll.maxValue > 0 &&
                    gridScroll.value < gridScroll.maxValue - hideThresholdPx
            }
        }
        Box(Modifier.weight(1f)) {
        Column(Modifier.fillMaxSize().verticalScroll(gridScroll)) {
            Spacer(Modifier.height(12.dp))
            AnimatedContent(
                targetState = week,
                transitionSpec = {
                    val forward = targetState > initialState
                    val duration = if (motionTokens.enabled) motionTokens.durationMediumMs else 0
                    val easing = motionTokens.easing.toComposeEasing()
                    (slideInHorizontally(
                        animationSpec = tween<IntOffset>(durationMillis = duration, easing = easing),
                    ) { if (forward) it / 5 else -it / 5 } + fadeIn(
                        tween<Float>(durationMillis = duration, easing = easing),
                    )) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween<IntOffset>(durationMillis = duration, easing = easing),
                        ) { if (forward) -it / 5 else it / 5 } + fadeOut(
                            tween<Float>(durationMillis = duration, easing = easing),
                        ))
                },
                label = "timetable_week",
            ) { animatedWeek ->
                val animatedStart = (timetable?.startTime?.time ?: currentWeekMondayMillis()) +
                    (animatedWeek - 1).toLong() * 7L * 24 * 3600 * 1000
                val animatedEvents by remember(service, animatedStart) {
                    service?.getEventsDuringWithColor(animatedStart, animatedStart + 7L * 24 * 3600 * 1000)
                        ?: androidx.lifecycle.MutableLiveData(emptyList<EventItem>())
                }.observeAsState(emptyList())
        Row(Modifier.fillMaxWidth().height(hourHeight * 14).pointerInput(Unit) {
            var drag = 0f
            detectHorizontalDragGestures(
                onDragStart = { drag = 0f },
                onHorizontalDrag = { _, amount -> drag += amount },
                onDragEnd = {
                    if (abs(drag) > 120) weekOffset += if (drag < 0) 1 else -1
                },
            )
        }) {
            Column(Modifier.width(48.dp)) {
                repeat(14) { index ->
                    Box(Modifier.fillMaxWidth().height(hourHeight), contentAlignment = Alignment.TopStart) {
                        Text("${index + 8}:00", color = shellTertiary(), fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxSize()) {
                    repeat(7) { dayIndex ->
                        Box(
                            Modifier.weight(1f).fillMaxHeight()
                                .background(if (dayIndex == todayIndex) Color(0x0D000000) else Color.Transparent),
                        )
                    }
                }
                repeat(14) { hour ->
                    Box(
                        Modifier.fillMaxWidth().offset(y = (hourHeight * hour)).height(1.dp)
                            .background(Color(0x11000000)),
                    )
                }
                Row(Modifier.fillMaxSize()) {
                    repeat(7) { dayIndex ->
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            animatedEvents.filter { dayOfWeek(it.from.time) == dayIndex + 1 }.forEach { item ->
                                val fromMinute = minuteOfDay(item.from.time)
                                val toMinute = minuteOfDay(item.to.time)
                                val top = ((fromMinute - 8 * 60).coerceAtLeast(0) / 60f) * hourHeight.value
                                val height = ((toMinute - fromMinute).coerceAtLeast(30) / 60f) * hourHeight.value
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (item.color != 0) Color(item.color) else Color(0xFFC24C8C),
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                                        .offset(y = top.dp).height(height.dp)
                                        .clickable {
                                            val activity = context as? androidx.fragment.app.FragmentActivity
                                            val uiService = (context.applicationContext as? HApplication)
                                                ?.pluginHost?.service(com.stupidtree.hita.api.TimetableUiService.KEY)
                                            if (activity != null && uiService != null) {
                                                uiService.showEventInfo(activity, listOf(item))
                                            }
                                        },
                                ) {
                                    Column(Modifier.fillMaxSize().padding(5.dp)) {
                                        Text(
                                            item.name,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            lineHeight = 15.sp,
                                            maxLines = 4,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(Modifier.weight(1f))
                                        item.place?.takeIf { it.isNotBlank() }?.let {
                                            Text(
                                                it,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                lineHeight = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
            }
            Spacer(Modifier.height(96.dp))
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (changeCount > 0) {
                TimetableChangePill(changeCount) {
                    pendingSnapshot?.let {
                        snapshotStore.save(snapshotKey, it)
                        baseline = it
                    }
                    pendingSnapshot = null
                    changeCount = 0
                }
            }
            if (showEveningHint) {
                TimetableEveningHintPill {
                    hintScope.launch { gridScroll.animateScrollTo(gridScroll.maxValue) }
                }
            }
        }
        }
    }
    if (showHistory && historyEnabled) {
        TimetableHistoryDialog(
            versions = historyVersions,
            currentEvents = historyEvents,
            onDismiss = { showHistory = false },
            onRestore = { version ->
                historyScope.launch {
                    val current = timetable ?: return@launch
                    val svc = service ?: return@launch
                    val all = withContext(Dispatchers.IO) {
                        TimetableHistoryDiff.eventsFor(
                            current.id,
                            svc.getEventsDuringSync(
                                current.startTime.time,
                                current.endTime.time.coerceAtLeast(
                                    current.startTime.time + 20L * 7 * 24 * 3600 * 1000,
                                ),
                            ),
                        )
                    }
                    val versions = withContext(Dispatchers.IO) {
                        historyStore.record(current.id, all, "RESTORE_BACKUP")
                        historyStore.record(current.id, version.events, "RESTORE")
                        svc.actionDeleteEvents(all)
                        svc.actionAddEvents(version.events)
                        historyStore.versions(current.id)
                    }
                    historyEvents = version.events
                    historyVersions = versions
                    showHistory = false
                }
            },
        )
    }
}

@Composable
private fun TimetableChangePill(count: Int, onDismiss: () -> Unit) {
    TimetableHintPill(
        icon = R.drawable.ic_baseline_settings_backup_restore_24,
        text = "课表已更新 · $count 处调整",
        onClick = onDismiss,
    )
}

@Composable
private fun TimetableEveningHintPill(onClick: () -> Unit) {
    TimetableHintPill(
        icon = R.drawable.ic_moon,
        text = "还有更多课程",
        onClick = onClick,
    )
}

@Composable
private fun TimetableHintPill(icon: Int, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

private fun minuteOfDay(time: Long): Int {
    val c = java.util.Calendar.getInstance()
    c.timeInMillis = time
    return c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)
}

private fun currentWeekMondayMillis(): Long {
    val calendar = java.util.Calendar.getInstance()
    calendar.add(java.util.Calendar.DAY_OF_YEAR, 1 - dayOfWeek(calendar.timeInMillis))
    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    calendar.set(java.util.Calendar.MINUTE, 0)
    calendar.set(java.util.Calendar.SECOND, 0)
    calendar.set(java.util.Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun dayOfWeek(time: Long): Int {
    val c = java.util.Calendar.getInstance()
    c.timeInMillis = time
    return when (c.get(java.util.Calendar.DAY_OF_WEEK)) {
        java.util.Calendar.SUNDAY -> 7
        else -> c.get(java.util.Calendar.DAY_OF_WEEK) - 1
    }
}

@Composable
private fun MorePage(
    service: TimetableService?,
    onOpenDrawer: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("local_eas_token", android.content.Context.MODE_PRIVATE) }
    val easAuth = remember(context) {
        (context.applicationContext as? HApplication)?.pluginHost
            ?.service(com.stupidtree.hita.api.EasAuthService.KEY)
    }
    var authRevision by remember { mutableIntStateOf(0) }
    var showEasLogin by remember { mutableStateOf(false) }
    val webLoginLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            authRevision++
            android.widget.Toast.makeText(context, "已连接深圳教务 Web 会话", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val userName = remember(authRevision) {
        easAuth?.accountName()?.takeIf { it.isNotBlank() }
            ?: prefs.getString("name", null)?.takeIf { it.isNotBlank() }
            ?: prefs.getString("username", null)?.takeIf { it.isNotBlank() }
    }
    val userSubtitle = remember(authRevision) {
        val id = easAuth?.accountSubtitle()?.takeIf { it.isNotBlank() }
            ?: prefs.getString("stuId", null)?.takeIf { it.isNotBlank() }
        val grade = prefs.getString("grade", null)?.takeIf(String::isNotBlank)
            ?: id?.take(4)?.takeIf { it.length == 4 }?.plus("级")
        val details = listOf(
            prefs.getString("school", null),
            prefs.getString("major", null),
            grade,
        ).filterNotNull().filter(String::isNotBlank).joinToString(" · ")
        listOfNotNull(id, details.takeIf { it.isNotBlank() })
            .joinToString("\n")
            .takeIf { it.isNotBlank() }
    }
    val timetableCountLive = remember(service) {
        service?.getTimetableCount() ?: androidx.lifecycle.MutableLiveData(0)
    }
    val timetableCount by timetableCountLive.observeAsState(0)
    fun open(className: String?) {
        if (className.isNullOrBlank()) return
        runCatching {
            context.startActivity(
                android.content.Intent().setClassName(context, className)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
    fun withEas(action: (com.stupidtree.hita.api.EasService, androidx.fragment.app.FragmentActivity) -> Unit) {
        val activity = context as? androidx.fragment.app.FragmentActivity
        val eas = (context.applicationContext as? HApplication)?.pluginHost
            ?.service(com.stupidtree.hita.api.EasService.KEY)
        if (activity != null && eas != null) {
            action(eas, activity)
        } else {
            android.widget.Toast.makeText(context, "EAS 插件未安装", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    fun openAccount() {
        val activity = context as? androidx.fragment.app.FragmentActivity
        val eas = (context.applicationContext as? HApplication)?.pluginHost
            ?.service(com.stupidtree.hita.api.EasService.KEY)
        when {
            easAuth != null -> showEasLogin = true
            activity != null && eas != null -> eas.openAccount(activity)
            else -> android.widget.Toast.makeText(context, "EAS 插件未安装", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    fun openPlugin(id: String, args: android.os.Bundle? = null) {
        val installed = runCatching {
            com.stupidtree.hitax.uiplugin.UiPluginManager(context).enabled().any { it.id == id }
        }.getOrDefault(false)
        if (!installed) {
            val record = com.stupidtree.hitax.uiplugin.UiPluginStore.find(context, id)
            val disabled = record != null &&
                com.stupidtree.hitax.uiplugin.UiPluginStore.isDisabled(context, id)
            com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle(if (disabled) "插件已停用" else "插件未安装")
                .setMessage(
                    if (disabled) "$id\n是否前往插件市场启用？"
                    else "$id\n是否前往插件市场安装？",
                )
                .setPositiveButton(if (disabled) "启用" else "安装") { _, _ ->
                    context.startActivity(
                        com.stupidtree.hitax.market.PluginMarketActivity.intent(
                            context,
                            id,
                            autoInstall = !disabled,
                        ),
                    )
                }
                .setNegativeButton("取消", null)
                .show()
            return
        }
        runCatching {
            context.startActivity(
                android.content.Intent(context, com.stupidtree.hitax.uiplugin.PluginUiActivity::class.java)
                    .putExtra(com.stupidtree.hitax.uiplugin.PluginUiActivity.EXTRA_PLUGIN_ID, id)
                    .putExtra(com.stupidtree.hitax.uiplugin.PluginUiActivity.EXTRA_ARGUMENTS, args)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
    val reminderStore = remember {
        com.stupidtree.hitax.data.source.preference.CourseReminderStore.getInstance(context)
    }
    var reminderEnabled by remember { mutableStateOf(reminderStore.isEnabled()) }
    var endReminderEnabled by remember { mutableStateOf(reminderStore.isEndReminderEnabled()) }
    val android33 = remember(context) {
        com.stupidtree.hitax.market.PresetStateStore.activeId(context)
            ?.startsWith("hita-android-3.3.") == true
    }
    var liveEnabled by remember {
        mutableStateOf(com.stupidtree.hitax.livecourse.LiveCourseSettings.isEnabled(context))
    }
    var usageEnabled by remember {
        mutableStateOf(com.stupidtree.hitax.data.source.preference.UsageStatsStore.isEnabled(context))
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            reminderStore.setEnabled(true)
            com.stupidtree.hitax.data.work.CourseReminderScheduler.autoSchedule(context)
            com.stupidtree.hitax.data.work.ExamReminderScheduler.autoSchedule(context)
            reminderEnabled = true
            android.widget.Toast.makeText(context, "课程提醒已开启（上课前15分钟提醒）", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            reminderEnabled = false
            android.widget.Toast.makeText(context, "需要通知权限才能发送课程提醒", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    fun setReminder(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        reminderStore.setEnabled(enabled)
        com.stupidtree.hitax.data.work.CourseReminderScheduler.autoSchedule(context)
        com.stupidtree.hitax.data.work.ExamReminderScheduler.autoSchedule(context)
        reminderEnabled = enabled
        android.widget.Toast.makeText(
            context,
            if (enabled) "课程提醒已开启（上课前15分钟提醒）" else "课程提醒已关闭",
            android.widget.Toast.LENGTH_SHORT,
        ).show()
    }
    val endReminderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            reminderStore.setEndReminderEnabled(true)
            com.stupidtree.hitax.data.work.CourseReminderScheduler.autoSchedule(context)
            com.stupidtree.hitax.data.work.ExamReminderScheduler.autoSchedule(context)
            endReminderEnabled = true
            android.widget.Toast.makeText(context, "下课前提醒已开启（默认提前5分钟）", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            endReminderEnabled = false
            android.widget.Toast.makeText(context, "需要通知权限才能发送下课前提醒", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    fun setEndReminder(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            endReminderLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        reminderStore.setEndReminderEnabled(enabled)
        com.stupidtree.hitax.data.work.CourseReminderScheduler.autoSchedule(context)
        com.stupidtree.hitax.data.work.ExamReminderScheduler.autoSchedule(context)
        endReminderEnabled = enabled
        android.widget.Toast.makeText(
            context,
            if (enabled) "下课前提醒已开启（默认提前5分钟）" else "下课前提醒已关闭",
            android.widget.Toast.LENGTH_SHORT,
        ).show()
    }
    val calendarScope = androidx.compose.runtime.rememberCoroutineScope()
    val syncTimetableService = remember { com.stupidtree.hitax.HitaServices.timetable(context) }
    val syncTimetable by (
        remember {
            syncTimetableService?.getRecentTimetable()
                ?: androidx.lifecycle.MutableLiveData(null)
        }
    ).observeAsState(null)
    fun runCalendarSync() {
        val service = syncTimetableService
        val timetable = syncTimetable
        if (service == null || timetable == null) {
            android.widget.Toast.makeText(context, "还没有可同步的课表", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        calendarScope.launch {
            val events = withContext(Dispatchers.IO) {
                service.getEventsDuringSync(
                    timetable.startTime.time,
                    timetable.endTime.time.coerceAtLeast(
                        timetable.startTime.time + 20L * 7 * 24 * 3600 * 1000,
                    ),
                )
            }
            val result = withContext(Dispatchers.IO) {
                com.stupidtree.hitax.calendar.CalendarSyncRepository(context)
                    .sync(timetable.id, timetable.name, events)
            }
            val message = when (result) {
                is com.stupidtree.hitax.calendar.CalendarSyncRepository.Result.Success ->
                    "已同步到系统日历：新增 ${result.inserted}，更新 ${result.updated}，删除 ${result.deleted}"
                com.stupidtree.hitax.calendar.CalendarSyncRepository.Result.PermissionDenied ->
                    "需要日历读写权限"
                com.stupidtree.hitax.calendar.CalendarSyncRepository.Result.NoWritableCalendar ->
                    "本机没有可写日历账户，请先在系统日历里添加账户"
                is com.stupidtree.hitax.calendar.CalendarSyncRepository.Result.Failed ->
                    "同步失败：${result.message}"
            }
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) runCalendarSync()
        else android.widget.Toast.makeText(context, "需要日历读写权限才能同步", android.widget.Toast.LENGTH_LONG).show()
    }
    fun requestCalendarSync() {
        val permissions = arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
        )
        if (permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        ) {
            runCalendarSync()
        } else {
            calendarPermissionLauncher.launch(permissions)
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("更多", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            val isNight = (context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            val theme = (context.applicationContext as HApplication).pluginHost
                .service(com.stupidtree.hita.api.ThemeController.KEY)
            Row(Modifier.offset(x = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { theme?.setNightMode(!isNight) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_moon_auto),
                        contentDescription = "切换深浅色",
                        tint = shellSecondary(),
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "打开抽屉", tint = shellSecondary())
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().clickable { openAccount() },
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.place_holder_avatar),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                )
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        userName ?: "未登录教务",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        userSubtitle ?: if (userName == null) "点击进行教务登录" else "",
                        color = shellSecondary(),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_keyboard_arrow_right_24),
                    contentDescription = null,
                    tint = shellTertiary(),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().height(80.dp).clickable {
                com.stupidtree.hitax.utils.ActivityUtils.startTimetableManager(context)
            },
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_list),
                        contentDescription = null,
                        tint = Color(0xFF3390EC),
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("课表管理", color = shellSecondary(), fontSize = 12.sp, maxLines = 1)
                    Text(
                        if ((timetableCount ?: 0) == 0) "无课表" else "共${timetableCount}个课表",
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        MoreGroup("教务功能") {
            MoreRow("考试", icon = R.drawable.ic_baseline_today_24) {
                withEas { eas, activity -> eas.openExam(activity) }
            }
            MoreRow("成绩管理", icon = R.drawable.ic_baseline_format_list_bulleted_24) {
                withEas { eas, activity -> eas.openScores(activity) }
            }
            MoreRow("个人成绩明细", "成绩状态 · 分项得分 · 权重折算", R.drawable.ic_baseline_format_list_bulleted_24) {
                openPlugin("com.stupidtree.hita.ui.score")
            }
            MoreRow("空教室", icon = R.drawable.ic_baseline_location_city_24) {
                withEas { eas, activity -> eas.openClassroom(activity) }
            }
            MoreRow("培养方案", "完成度 · 学分类别 · 培养课组", R.drawable.ic_baseline_format_list_bulleted_24) {
                openPlugin("com.stupidtree.hita.ui.training")
            }
            MoreRow("深圳课程浏览", "教务选课池 · 全校课表（需 Web 登录）", R.drawable.ic_baseline_search_24) {
                openPlugin("com.stupidtree.hita.ui.community")
            }
        }
        Spacer(Modifier.height(8.dp))
        MoreGroup("课程资源") {
            MoreRow("查询资源", "查询三个校区的课程资料", R.drawable.ic_baseline_search_24) {
                open("com.stupidtree.hitax.ui.search.SearchActivity")
            }
            MoreRow("提交课程资料 PR", "欢迎关注 HITCS 和 HOA，谢谢喵", R.drawable.ic_baseline_edit_24) {
                val service = com.stupidtree.hitax.HitaServices.resourceUi(context)
                if (service == null) {
                    android.widget.Toast.makeText(context, "课程资源插件未安装", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    service.openSearch(context, null, submit = true)
                }
            }
            MoreRow("实用网址", "常用校内网站，点按打开", R.drawable.ic_baseline_link_24) {
                openPlugin("com.stupidtree.hita.ui.links")
            }
        }
        Spacer(Modifier.height(8.dp))
        MoreGroup("设置") {
            MoreToggleRow(
                "课程提醒",
                "上课前15分钟提醒",
                R.drawable.ic_baseline_access_alarm_24,
                reminderEnabled,
            ) { setReminder(it) }
            if (android33) {
                MoreToggleRow(
                    "下课前提醒",
                    "下课前5分钟提醒（3.3.x）",
                    R.drawable.ic_baseline_settings_backup_restore_24,
                    endReminderEnabled,
                ) { setEndReminder(it) }
                MoreRow(
                    "同步到系统日历",
                    "将教务课程写入本机日历（3.3.x）",
                    R.drawable.ic_baseline_date_range_24,
                ) { requestCalendarSync() }
                MoreRow(
                    "待办聚合",
                    "Grader 与 GeniusTA 合并待办（3.3.x）",
                    R.drawable.ic_baseline_check_24,
                ) { openPlugin("com.stupidtree.hita.ui.todo") }
                MoreRow(
                    "校园卡",
                    "余额与消费流水（3.3.x）",
                    R.drawable.ic_bx_credit,
                ) { openPlugin("com.stupidtree.hita.ui.campus", android.os.Bundle().apply { putString("target", "card"); putString("title", "校园卡") }) }
                MoreRow(
                    "静音仓",
                    "空间与预约（3.3.x）",
                    R.drawable.ic_baseline_access_time_24,
                ) { openPlugin("com.stupidtree.hita.ui.campus", android.os.Bundle().apply { putString("target", "pod"); putString("title", "静音仓") }) }
                MoreRow(
                    "图书馆",
                    "智慧墙与研习间（3.3.x）",
                    R.drawable.ic_baseline_search_24,
                ) { openPlugin("com.stupidtree.hita.ui.campus", android.os.Bundle().apply { putString("target", "library"); putString("title", "大学城图书馆") }) }
                MoreRow(
                    "实验中心",
                    "实验课进度（3.3.x）",
                    R.drawable.ic_circle_done,
                ) { openPlugin("com.stupidtree.hita.ui.campus", android.os.Bundle().apply { putString("target", "lab"); putString("title", "实验中心") }) }
            }
            MoreToggleRow(
                "实时课程",
                "上课前10分钟显示课程状态",
                R.drawable.ic_baseline_access_time_24,
                liveEnabled,
            ) { enabled ->
                com.stupidtree.hitax.livecourse.LiveCourseSettings.setEnabled(context, enabled)
                liveEnabled = enabled
                android.widget.Toast.makeText(
                    context,
                    if (enabled) "实时课程已开启" else "实时课程已关闭",
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
            MoreToggleRow(
                "使用统计",
                "匿名统计功能使用情况，帮助改进应用",
                R.drawable.ic_info,
                usageEnabled,
            ) { enabled ->
                com.stupidtree.hitax.data.source.preference.UsageStatsStore.setEnabled(context, enabled)
                usageEnabled = enabled
            }
            MoreRow("清除使用统计", "删除本机事件队列与匿名安装 ID", R.drawable.ic_baseline_delete_24) {
                com.stupidtree.hitax.analytics.UsageAnalytics.clear(context)
                android.widget.Toast.makeText(context, "使用统计已清除", android.widget.Toast.LENGTH_SHORT).show()
            }
            MoreRow("公告", "版本更新 · 服务与故障通知", R.drawable.ic_menu_settings) {
                openPlugin("com.stupidtree.hita.ui.announcement")
            }
            MoreRow("插件市场", "切换整合包、安装或停用插件", R.drawable.ic_menu_settings) {
                context.startActivity(
                    android.content.Intent(
                        context,
                        com.stupidtree.hitax.market.PluginMarketActivity::class.java,
                    )
                )
            }
            MoreRow("应用设置", "外观、字体与界面", R.drawable.ic_menu_settings) {
                openPlugin("com.stupidtree.hita.ui.appearance")
            }
        }
    }
    if (showEasLogin && easAuth != null) {
        EasLoginDialog(
            auth = easAuth,
            onDismiss = { showEasLogin = false },
            onSuccess = {
                showEasLogin = false
                authRevision++
                android.widget.Toast.makeText(context, "教务登录成功", android.widget.Toast.LENGTH_SHORT).show()
            },
            onWebLogin = {
                showEasLogin = false
                webLoginLauncher.launch(
                    android.content.Intent(context, ShellEasWebLoginActivity::class.java)
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EasLoginDialog(
    auth: com.stupidtree.hita.api.EasAuthService,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onWebLogin: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var campusId by remember { mutableStateOf(CampusCatalog.default.id) }
    val campus = CampusCatalog.byId(campusId) ?: CampusCatalog.default
    var agreed by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var request by remember { mutableStateOf<androidx.lifecycle.LiveData<com.stupidtree.component.data.DataState<Boolean>>?>(null) }
    val fallback = remember { androidx.lifecycle.MutableLiveData<com.stupidtree.component.data.DataState<Boolean>>() }
    val result by (request ?: fallback).observeAsState()
    LaunchedEffect(result) {
        val state = result ?: return@LaunchedEffect
        if (state.state == com.stupidtree.component.data.DataState.STATE.SUCCESS && state.data == true) {
            onSuccess()
        } else {
            val message = state.message.orEmpty()
            error = when {
                message.contains("timeout", ignoreCase = true) ||
                    message.contains("timed out", ignoreCase = true) ->
                    "网络超时，请检查校园网或 VPN 后重试"
                message.isBlank() -> "登录失败，请检查账号密码"
                else -> message
            }
            loading = false
        }
    }
    ModalBottomSheet(
        onDismissRequest = { if (!loading) onDismiss() },
        containerColor = if (isSystemInDarkTheme()) Color(0xFF121923) else Color(0xFFF7FAFF),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("教务认证", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("请登录教务账号以继续", fontSize = 16.sp, color = shellSecondary())
                }
                Button(
                    onClick = {
                        loading = true
                        error = null
                        request = auth.login(username.trim(), password)
                    },
                    enabled = !loading && agreed && campus.supportsApiLogin &&
                        username.isNotBlank() && password.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(36.dp),
                ) {
                    Text(if (loading) "登录中…" else "API 登录", fontSize = 18.sp)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                CampusCatalog.all.forEachIndexed { index, provider ->
                    if (index > 0) Spacer(Modifier.width(12.dp))
                    androidx.compose.material3.RadioButton(
                        selected = campus.id == provider.id,
                        onClick = { campusId = provider.id },
                        enabled = !loading,
                    )
                    Text(provider.displayName, fontSize = 14.sp, modifier = Modifier.padding(start = 2.dp))
                }
            }
            campus.unavailableNote?.let {
                Text(
                    it,
                    fontSize = 12.sp,
                    color = shellSecondary(),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (campus.supportsApiLogin) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                singleLine = true,
                label = { Text("学号 / 用户名") },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                label = { Text("密码") },
                enabled = !loading,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            }
            androidx.compose.material3.OutlinedButton(
                onClick = onWebLogin,
                enabled = !loading,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("使用统一身份认证网页登录")
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(
                    checked = agreed,
                    onCheckedChange = { agreed = it },
                    enabled = !loading,
                )
                Text("登录即表示同意用户协议与隐私政策", fontSize = 12.sp, color = shellSecondary())
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss, enabled = !loading) { Text("稍后") }
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }
    }

}

@Composable
private fun MoreGroup(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun MoreRow(
    title: String,
    subtitle: String? = null,
    icon: Int,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.ic_baseline_keyboard_arrow_right_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MoreToggleRow(
    title: String,
    subtitle: String,
    icon: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MoreCard(title: String, subtitle: String, onClick: () -> Unit) {
    val shape = hitaStyleCardShape(18.dp, 10.dp)
    Card(
        shape = shape,
        colors = hitaGlassCardColors(),
        border = hitaGlassCardBorder(),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            .hitaGlassCardModifier(shape, 6.dp),
        onClick = onClick,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(subtitle, color = shellSecondary(), fontSize = 12.sp)
        }
    }
}
