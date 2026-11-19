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
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.text.style.TextOverflow
import com.stupidtree.hita.api.FeedbackEvent
import com.stupidtree.hita.api.FeedbackService
import com.stupidtree.hita.api.FeedbackSettings
import com.stupidtree.hita.api.MotionController
import com.stupidtree.hita.api.MotionEasing
import com.stupidtree.hita.api.MotionTokens
import com.stupidtree.hita.api.TimetableService
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.R
import com.stupidtree.hitax.data.model.timetable.EventItem
import com.stupidtree.hitax.shell.homework.HomeworkRepository
import com.stupidtree.hitax.shell.homework.HomeworkScreen
import com.stupidtree.hitax.agent.AgentChatViewModel
import com.stupidtree.hitax.shell.agent.AgentChatScreen
import com.stupidtree.hitax.shell.homework.HomeworkViewModel
import com.stupidtree.hitax.shell.news.ShellNewsPage
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
            val scheme = if (dark) {
                androidx.compose.material3.darkColorScheme(
                    primary = Color(0xFF6FB3F2),
                    onPrimary = Color(0xFF001B33),
                    primaryContainer = Color(0xFF1E3A5F),
                    onPrimaryContainer = Color(0xFFD6E4F5),
                    background = Color(0xFF101317),
                    surface = Color(0xFF1A1E24),
                    onSurface = Color(0xFFF2F4F7),
                    onSurfaceVariant = Color(0xFFAEB6C2),
                )
            } else {
                androidx.compose.material3.lightColorScheme(
                    primary = Color(0xFF3390EC),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFEBF4FC),
                    onPrimaryContainer = Color(0xFF003366),
                    background = Color(0xFFF6F7FA),
                    surface = Color.White,
                    onSurface = Color.Black,
                    onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MaterialTheme(colorScheme = scheme) {
                Surface(Modifier.fillMaxSize(), color = scheme.background) {
                    HitaAndroidShell(service, homeworkViewModel, agentViewModel)
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
            "hita.timetable" -> ShellTarget.TIMETABLE
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
        )
    }
}

@Composable
private fun HitaAndroidShell(
    service: TimetableService?,
    homeworkViewModel: HomeworkViewModel,
    agentViewModel: AgentChatViewModel,
) {
    val shellContext = androidx.compose.ui.platform.LocalContext.current
    val tabs = remember(shellContext) { loadShellTabs(shellContext) }
    var selectedId by remember(tabs) {
        mutableStateOf(tabs.firstOrNull { it.id == "hita.timeline" }?.id ?: tabs.firstOrNull()?.id)
    }
    val selected = tabs.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 } ?: 0
    var drawerOpen by remember { mutableStateOf(false) }
    var tabEditorOpen by remember { mutableStateOf(false) }
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
    if (tabEditorOpen) {
        ShellTabEditor(
            onDismiss = { tabEditorOpen = false },
            onSaved = {
                tabEditorOpen = false
                (shellContext as? android.app.Activity)?.recreate()
            },
        )
    }
    BackHandler(enabled = drawerOpen) { drawerOpen = false }
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(tabSpec) togetherWith fadeOut(tabSpec) },
                label = "shell_tab",
            ) { index ->
                val tab = tabs.getOrNull(index)
                when (tab?.target) {
                    ShellTarget.TODO -> HomeworkScreen(homeworkViewModel)
                    ShellTarget.TODAY -> TodayPage(service, motionTokens)
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
                        onEditTabs = { tabEditorOpen = true },
                    )
                    ShellTarget.PLUGIN -> tab.pluginTab?.let {
                        RuntimeUiTabPage(tab.id, it, tab.title)
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
            val indicatorOffset by animateDpAsState(
                targetValue = tabWidth * selected,
                label = "shell_tab_indicator",
            )
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                modifier = Modifier.width(barWidth).height(54.dp),
            ) {
                Box(Modifier.fillMaxSize().padding(4.dp)) {
                    Surface(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(tabWidth)
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF3390EC).copy(alpha = 0.12f),
                    ) {}
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        tabs.forEachIndexed { index, tab ->
                            val active = selected == index
                            val tint = if (active) Color(0xFF0088CC) else MaterialTheme.colorScheme.onSurface
                            Column(
                                Modifier
                                    .width(tabWidth)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable {
                                        selectedId = tab.id
                                        feedback?.play(FeedbackEvent.TAB)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    painter = painterResource(tab.iconRes),
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    tab.title,
                                    color = tint,
                                    fontSize = 10.sp,
                                    lineHeight = 10.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    ShellDrawerOverlay(drawerOpen, motionTokens) { drawerOpen = false }
    }
}

@Composable
private fun ShellDrawerOverlay(open: Boolean, motionTokens: MotionTokens, onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val eas = remember {
        (context.applicationContext as? HApplication)?.pluginHost?.service(
            com.stupidtree.hita.api.EasService.KEY,
        )
    }
    val userName = eas?.accountName()
    val userSubtitle = eas?.accountSubtitle()?.takeIf { it.isNotBlank() }
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
    val drawerDuration = if (motionTokens.enabled) motionTokens.durationMediumMs else 0
    val drawerEasing = motionTokens.easing.toComposeEasing()
    AnimatedVisibility(
        visible = open,
        modifier = Modifier.fillMaxSize(),
        enter = slideInHorizontally(
            animationSpec = tween<IntOffset>(durationMillis = drawerDuration, easing = drawerEasing),
        ) { it } + fadeIn(tween<Float>(durationMillis = drawerDuration, easing = drawerEasing)),
        exit = slideOutHorizontally(
            animationSpec = tween<IntOffset>(durationMillis = drawerDuration, easing = drawerEasing),
        ) { it } + fadeOut(tween<Float>(durationMillis = drawerDuration, easing = drawerEasing)),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f))
                    .clickable { onClose() },
            )
            Surface(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(260.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
            ) {
                Column(
                    Modifier.fillMaxSize().statusBarsPadding()
                        .padding(horizontal = 16.dp),
                ) {
                    Column(
                        Modifier.fillMaxWidth()
                            .clickable { launch("com.stupidtree.hitax.ui.eas.login.ShenzhenWebLoginActivity") }
                            .padding(top = 78.dp, bottom = 13.dp),
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
                            color = shellSecondary(),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(Color(0xFFE5E7EB)),
                    )
                    DrawerRow(
                        title = "界面风格 · $styleLabel",
                        icon = Icons.Default.Palette,
                    ) {
                        context.startActivity(
                            android.content.Intent(context, com.stupidtree.hitax.uiplugin.PluginUiActivity::class.java)
                                .putExtra(
                                    com.stupidtree.hitax.uiplugin.PluginUiActivity.EXTRA_PLUGIN_ID,
                                    "com.stupidtree.hita.ui.appearance",
                                )
                        )
                        onClose()
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
                        icon = Icons.Default.Edit,
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
                        color = shellSecondary(),
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "茶蘼 | 离谱 | 北极熊 | 软糯 | 纪行",
                        color = Color(0xFF2F80ED),
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "RadiantAbyss",
                        color = Color(0xFF2F80ED),
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Limpu 的赞赏码",
                        color = shellTertiary(),
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                }
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
        Modifier.fillMaxWidth().height(56.dp).clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            icon != null -> androidx.compose.material3.Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(24.dp),
            )
            iconRes != null -> androidx.compose.material3.Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            title,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 13.dp),
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
private fun TodayPage(service: TimetableService?, motionTokens: MotionTokens) {
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
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(39.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C5C91)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        ) {
            Column(
                Modifier.fillMaxWidth().background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(Color(0xFF1C5E9D), Color(0xFF0C5C91)),
                    ),
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
                        color = Color.White,
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
                        color = Color.White.copy(alpha = 0.8f),
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
                            .background(Color.White.copy(alpha = 0.16f))
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
                    tint = Color.White,
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
                            .background(Color.White.copy(alpha = 0.16f))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                countdown ?: "接下来没事了",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                nextEvent?.name ?: "see you",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                            )
                        }
                        androidx.compose.material3.Icon(
                            painter = painterResource(
                                if (nextEvent != null) R.drawable.ic_baseline_access_alarm_24 else R.drawable.ic_empty,
                            ),
                            contentDescription = null,
                            tint = Color.White,
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
        (timetable?.startTime?.time ?: 0L) + (week - 1).toLong() * 7 * 24 * 3600 * 1000
    }
    val eventsLive = remember(service, start) {
        service?.getEventsDuring(start, start + 7L * 24 * 3600 * 1000)
            ?: androidx.lifecycle.MutableLiveData(emptyList())
    }
    val events by eventsLive.observeAsState(emptyList())

    if (timetable == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.ic_timeline),
                    contentDescription = null,
                    tint = Color(0xFF3390EC).copy(alpha = 0.35f),
                    modifier = Modifier.size(96.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text("还没有课表", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text("请先在功能中心导入课表", color = shellTertiary(), fontSize = 12.sp)
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        val context = LocalContext.current
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 13.5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "第${week}周",
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
            )
            Spacer(Modifier.width(16.dp))
            Text(
                timetable?.name ?: "默认课表1",
                color = shellSecondary(),
                fontSize = 14.sp,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
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
            }) { Icon(Icons.Default.Share, "分享课表", tint = shellSecondary()) }
            IconButton(onClick = {
                val activity = context as? androidx.fragment.app.FragmentActivity
                val uiService = (context.applicationContext as? HApplication)
                    ?.pluginHost?.service(com.stupidtree.hita.api.TimetableUiService.KEY)
                if (activity != null && uiService != null) uiService.openStylePanel(activity)
            }) {
                Icon(Icons.Default.Settings, "课表设置", tint = shellSecondary())
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
        LaunchedEffect(week) { gridScroll.scrollTo(0) }
        Column(Modifier.weight(1f).verticalScroll(gridScroll)) {
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
                val animatedStart = (timetable?.startTime?.time ?: 0L) +
                    (animatedWeek - 1).toLong() * 7L * 24 * 3600 * 1000
                val animatedEvents by remember(service, animatedStart) {
                    service?.getEventsDuring(animatedStart, animatedStart + 7L * 24 * 3600 * 1000)
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
                                        Text(item.name, color = Color.White, fontSize = 11.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.weight(1f))
                                        item.place?.takeIf { it.isNotBlank() }?.let {
                                            Text(it, color = Color.White, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    }
}

private fun minuteOfDay(time: Long): Int {
    val c = java.util.Calendar.getInstance()
    c.timeInMillis = time
    return c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)
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
    onEditTabs: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("local_eas_token", android.content.Context.MODE_PRIVATE) }
    val userName = remember {
        prefs.getString("name", null)?.takeIf { it.isNotBlank() }
            ?: prefs.getString("username", null)?.takeIf { it.isNotBlank() }
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
    fun openPlugin(id: String) {
        val installed = runCatching {
            com.stupidtree.hitax.uiplugin.UiPluginManager(context).enabled().any { it.id == id }
        }.getOrDefault(false)
        if (!installed) {
            android.widget.Toast.makeText(context, "插件未安装：$id", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            context.startActivity(
                android.content.Intent(context, com.stupidtree.hitax.uiplugin.PluginUiActivity::class.java)
                    .putExtra(com.stupidtree.hitax.uiplugin.PluginUiActivity.EXTRA_PLUGIN_ID, id)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
    val reminderStore = remember {
        com.stupidtree.hitax.data.source.preference.CourseReminderStore.getInstance(context)
    }
    var reminderEnabled by remember { mutableStateOf(reminderStore.isEnabled()) }
    var liveEnabled by remember {
        mutableStateOf(com.stupidtree.hitax.livecourse.LiveCourseSettings.isEnabled(context))
    }
    var usageEnabled by remember {
        mutableStateOf(com.stupidtree.hitax.data.source.preference.UsageStatsStore.isEnabled(context))
    }
    val feedback = remember {
        (context.applicationContext as? HApplication)?.pluginHost?.service(FeedbackService.KEY)
    }
    var feedbackSettings by remember {
        mutableStateOf(feedback?.settings() ?: FeedbackSettings())
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            reminderStore.setEnabled(true)
            com.stupidtree.hitax.data.work.CourseReminderScheduler.autoSchedule(context)
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
        reminderEnabled = enabled
        android.widget.Toast.makeText(
            context,
            if (enabled) "课程提醒已开启（上课前15分钟提醒）" else "课程提醒已关闭",
            android.widget.Toast.LENGTH_SHORT,
        ).show()
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
            IconButton(onClick = { theme?.setNightMode(!isNight) }) {
                Icon(
                    Icons.Default.DarkMode,
                    contentDescription = "切换深浅色",
                    tint = shellSecondary(),
                )
            }
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Default.Menu, contentDescription = "打开抽屉", tint = shellSecondary())
            }
        }
        Spacer(Modifier.height(22.dp))
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().clickable {
                open("com.stupidtree.hitax.ui.eas.login.ShenzhenWebLoginActivity")
            },
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 10.5.dp, end = 18.dp, top = 18.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.width(44.dp).height(44.dp).background(Color(0xFF2563EB), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(userName ?: "未登录教务", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text(
                        if (userName == null) "点击进行教务登录" else "已连接教务",
                        color = shellSecondary(),
                        fontSize = 12.sp,
                    )
                }
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = shellTertiary(),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().clickable {
                com.stupidtree.hitax.utils.ActivityUtils.startTimetableManager(context)
            },
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 10.5.dp, end = 18.dp, top = 18.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF3390EC),
                        modifier = Modifier.size(21.dp),
                    )
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text("课表管理", color = shellSecondary(), fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(if ((timetableCount ?: 0) == 0) "无课表" else "共${timetableCount}个课表", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                }
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = shellTertiary(),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(19.5.dp))
        MoreGroup("教务功能") {
            Spacer(Modifier.height(4.dp))
            MoreRow("考试", icon = Icons.Default.CalendarToday, rowHeight = 62.dp) { open("com.stupidtree.hitax.ui.eas.exam.ExamActivity") }
            MoreRow("成绩管理", icon = Icons.Default.Assessment, rowHeight = 42.dp) {
                open("com.stupidtree.hitax.ui.eas.score.ScoreInquiryActivity")
            }
            MoreRow("个人成绩明细", "成绩状态 · 分项得分 · 权重折算", Icons.Default.Assessment, rowHeight = 70.dp) {
                openPlugin("com.stupidtree.hita.ui.score")
            }
            MoreRow("空教室", icon = Icons.Default.MeetingRoom, rowHeight = 48.dp) { open("com.stupidtree.hitax.ui.eas.classroom.EmptyClassroomActivity") }
            MoreRow("培养方案", "完成度 · 学分类别 · 培养课组", Icons.Default.School, rowHeight = 56.dp) {
                openPlugin("com.stupidtree.hita.ui.training")
            }
            MoreRow("深圳课程浏览", "教务选课池 · 全校课表（需 Web 登录）", Icons.Default.Search, rowHeight = 67.dp) {
                openPlugin("com.stupidtree.hita.ui.community")
            }
        }
        Spacer(Modifier.height(14.dp))
        MoreGroup("课程资源") {
            MoreRow("查询资源", "查询三个校区的课程资料", Icons.Default.Search) {
                open("com.stupidtree.hitax.ui.search.SearchActivity")
            }
            MoreRow("提交课程资料 PR", "欢迎关注 HITCS 和 HOA，谢谢喵", Icons.Default.Edit) {
                val service = com.stupidtree.hitax.HitaServices.resourceUi(context)
                if (service == null) {
                    android.widget.Toast.makeText(context, "课程资源插件未安装", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    service.openSearch(context, null, submit = true)
                }
            }
            MoreRow("实用网址", "常用校内网站，点按打开", Icons.Default.Link) {
                openPlugin("com.stupidtree.hita.ui.links")
            }
        }
        Spacer(Modifier.height(14.dp))
        MoreGroup("设置") {
            MoreRow("编辑底栏", "拖拽排序、改名、换图标与隐藏", Icons.Default.Settings) {
                onEditTabs()
            }
            MoreToggleRow(
                "课程提醒",
                "上课前15分钟提醒",
                Icons.Default.CalendarToday,
                reminderEnabled,
            ) { setReminder(it) }
            MoreToggleRow(
                "实时课程",
                "上课前10分钟显示课程状态",
                Icons.Default.PlayArrow,
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
                "界面音效",
                "点击与切换的反馈音",
                Icons.Default.Notifications,
                feedbackSettings.soundEnabled,
            ) { enabled ->
                val next = feedbackSettings.copy(soundEnabled = enabled)
                feedback?.updateSettings(next)
                feedbackSettings = next
                if (enabled) feedback?.play(FeedbackEvent.CLICK)
            }
            MoreToggleRow(
                "触感反馈",
                "轻触与操作振动",
                Icons.Default.PlayArrow,
                feedbackSettings.hapticEnabled,
            ) { enabled ->
                val next = feedbackSettings.copy(hapticEnabled = enabled)
                feedback?.updateSettings(next)
                feedbackSettings = next
                if (enabled) feedback?.haptic(com.stupidtree.hita.api.HapticType.LIGHT)
            }
            if (feedbackSettings.soundEnabled) {
                Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 18.dp, top = 2.dp, bottom = 8.dp)) {
                    Text("界面音量 ${(feedbackSettings.volume * 100).toInt()}%", fontSize = 13.sp, color = shellSecondary())
                    Slider(
                        value = feedbackSettings.volume,
                        onValueChange = { value ->
                            val next = feedbackSettings.copy(volume = value)
                            feedbackSettings = next
                            feedback?.updateSettings(next)
                        },
                        valueRange = 0f..1f,
                    )
                }
            }
            MoreToggleRow(
                "使用统计",
                "匿名统计功能使用情况，帮助改进应用",
                Icons.Default.Info,
                usageEnabled,
            ) { enabled ->
                com.stupidtree.hitax.data.source.preference.UsageStatsStore.setEnabled(context, enabled)
                usageEnabled = enabled
            }
            MoreRow("清除使用统计", "删除本机事件队列与匿名安装 ID", Icons.Default.Delete) {
                com.stupidtree.hitax.analytics.UsageAnalytics.clear(context)
                android.widget.Toast.makeText(context, "使用统计已清除", android.widget.Toast.LENGTH_SHORT).show()
            }
            MoreRow("公告", "版本更新 · 服务与故障通知", Icons.Default.Notifications) {
                openPlugin("com.stupidtree.hita.ui.announcement")
            }
            MoreRow("插件市场", "切换整合包、安装或停用插件", Icons.Default.Settings) {
                context.startActivity(
                    android.content.Intent(
                        context,
                        com.stupidtree.hitax.market.PluginMarketActivity::class.java,
                    )
                )
            }
            MoreRow("应用设置", "外观、字体与界面", Icons.Default.Settings) {
                openPlugin("com.stupidtree.hita.ui.appearance")
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
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp),
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
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.ChevronRight,
    rowHeight: androidx.compose.ui.unit.Dp? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier.then(
            if (rowHeight != null) Modifier.heightIn(min = rowHeight) else Modifier
        ).fillMaxWidth().clickable(onClick = onClick)
            .padding(start = 20.dp, end = 18.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color(0xFF3390EC), modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = shellTertiary(), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MoreToggleRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }
            .padding(start = 20.dp, end = 18.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color(0xFF3390EC), modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun MoreCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        onClick = onClick,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(subtitle, color = shellSecondary(), fontSize = 12.sp)
        }
    }
}
