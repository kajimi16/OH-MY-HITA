package com.stupidtree.hita.ui.resource

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
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
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.MutableLiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.HostUiService
import com.stupidtree.hita.api.ResourceDataService
import com.stupidtree.hitax.data.model.resource.CourseReadmeData
import com.stupidtree.hitax.data.model.resource.CourseResourceItem
import io.noties.markwon.Markwon
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin

/**
 * 课程资源（运行时界面插件）。
 *
 * 视觉对齐内置 CourseResourceSearchActivity / CourseReadmeActivity：
 * 颜色取宿主主题属性，尺寸/字号按内置布局复刻。
 */
class ResourceUiPlugin : ComposeUiPlugin {

    override val id = "com.stupidtree.hita.ui.resource"

    override val displayName = "课程资源"

    override val requiresApi = 3

    override fun createView(context: Context, host: PluginUiHost): View {
        val initialQuery = host.arguments.getString("query").orEmpty()
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    ResourceApp(host, initialQuery)
                }
            }
        }
    }
}

/** 宿主主题里解析出来的颜色与尺寸。 */
private data class Tokens(
    val backgroundBottom: Color,
    val backgroundSecond: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val primary: Color,
    val actionBarHeight: Dp,
)

@Composable
private fun rememberTokens(): Tokens {
    val context = LocalContext.current
    return remember {
        Tokens(
            backgroundBottom = Color(context.attrColor("backgroundColorBottom", 0xFFF6F7FA.toInt())),
            backgroundSecond = Color(context.attrColor("backgroundColorSecond", 0xFFFFFFFF.toInt())),
            textPrimary = Color(context.attrColor("textColorPrimary", 0xFF000000.toInt())),
            textSecondary = Color(context.attrColor("textColorSecondary", 0xFF808080.toInt())),
            primary = Color(context.androidAttrColor(android.R.attr.colorPrimary, 0xFF304FFE.toInt())),
            actionBarHeight = context.attrDimen("actionBarSize", 56f),
        )
    }
}

private fun Context.androidAttrColor(attrId: Int, fallback: Int): Int {
    val value = TypedValue()
    if (!theme.resolveAttribute(attrId, value, true)) return fallback
    if (value.resourceId != 0) {
        return runCatching { getColorStateList(value.resourceId).defaultColor }.getOrElse { fallback }
    }
    return if (value.data != 0) value.data else fallback
}

private fun Context.attrColor(name: String, fallback: Int): Int {
    val id = if (name.startsWith("android:")) {
        resources.getIdentifier(name.removePrefix("android:"), "attr", "android")
    } else {
        resources.getIdentifier(name, "attr", packageName)
    }
    if (id == 0) return fallback
    val value = TypedValue()
    if (!theme.resolveAttribute(id, value, true)) return fallback
    return if (value.resourceId != 0) getColor(value.resourceId) else value.data
}

private fun Context.drawableId(name: String, pkg: String = packageName): Int =
    resources.getIdentifier(name, "drawable", pkg)

/** 直接读宿主资源里的文案，避免插件复制一份字符串。 */
private fun Context.hostString(name: String, fallback: String): String {
    val id = resources.getIdentifier(name, "string", packageName)
    return if (id != 0) getString(id) else fallback
}

/**
 * 用宿主 TextView 的字体度量计算行高（px → sp）。
 * 内置页面是 TextView 渲染，Compose 默认行高与它不一致；这样直接跟随平台参数。
 */
private fun Context.hostLineHeight(sp: Float): androidx.compose.ui.unit.TextUnit {
    val view = TextView(this)
    view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
    view.includeFontPadding = true
    val fm = view.paint.fontMetrics
    val px = fm.descent - fm.ascent + fm.leading
    return (px / resources.displayMetrics.scaledDensity).sp
}

private fun Context.attrDimen(name: String, fallbackDp: Float): Dp {
    val id = resources.getIdentifier(name, "attr", packageName)
    if (id == 0) return fallbackDp.dp
    val value = TypedValue()
    if (!theme.resolveAttribute(id, value, true)) return fallbackDp.dp
    val px = value.getDimension(resources.displayMetrics)
    return (px / resources.displayMetrics.density).dp
}

@Composable
private fun ResourceApp(host: PluginUiHost, initialQuery: String) {
    val service = remember { host.service(ResourceDataService.KEY) }
    val tokens = rememberTokens()
    var openReadme by remember { mutableStateOf<CourseResourceItem?>(null) }
    var input by remember { mutableStateOf(initialQuery) }
    var submitted by remember { mutableStateOf(initialQuery) }

    BackHandler(enabled = openReadme != null) { openReadme = null }

    if (openReadme == null) {
        SearchScreen(
            service = service,
            tokens = tokens,
            input = input,
            submitted = submitted,
            onInputChange = { input = it },
            onSubmit = { submitted = it },
            onOpenReadme = { openReadme = it },
        )
    } else {
        ReadmeScreen(service, host.service(HostUiService.KEY), tokens, openReadme!!)
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
private fun SearchScreen(
    service: ResourceDataService?,
    tokens: Tokens,
    input: String,
    submitted: String,
    onInputChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onOpenReadme: (CourseResourceItem) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var refreshToken by remember { mutableStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }
    val empty = remember { MutableLiveData<DataState<List<CourseResourceItem>>>() }
    val live = remember(submitted, refreshToken, service) {
        if (service == null || submitted.isBlank()) null else service.searchCourses(submitted)
    }
    val result by (live ?: empty).observeAsState()
    val generation by remember(submitted, refreshToken) { derivedStateOf { "$submitted#$refreshToken" } }
    val played = remember(generation) { mutableStateMapOf<Int, Boolean>() }
    LaunchedEffect(result?.state) {
        if (result?.state != null) refreshing = false
    }

    Column(Modifier.fillMaxSize().background(tokens.backgroundBottom)) {
        TopBar(tokens, title = LocalContext.current.hostString("course_resource_title", "课程资源"), showBack = false, onBack = {})
        SearchBar(
            tokens = tokens,
            value = input,
            onValueChange = onInputChange,
            onSearch = {
                keyboard?.hide()
                refreshing = true
                onSubmit(input.trim())
            },
        )
        val pullState = rememberPullRefreshState(
            refreshing = refreshing,
            onRefresh = {
                refreshing = true
                refreshToken++
            },
        )
        Box(Modifier.weight(1f).fillMaxWidth().pullRefresh(pullState)) {
            when {
                service == null -> CenterHint(tokens, LocalContext.current.hostString("course_resource_failed", "资源服务不可用"))
                submitted.isBlank() -> Unit
                result == null -> Unit // 搜索中由顶部 PullRefreshIndicator 指示
                result?.state != DataState.STATE.SUCCESS ->
                    CenterHint(tokens, "课程资源加载失败：${result?.message ?: result?.state}")
                result?.data.isNullOrEmpty() ->
                    CenterHint(tokens, LocalContext.current.hostString("course_resource_empty", "没有找到相关课程资源"))
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(result?.data ?: emptyList()) { index, item ->
                        Box(Modifier.animateItemPlacement()) {
                            EntranceAnimation(
                                index = index,
                                generation = generation,
                                played = played,
                            ) {
                                CourseCard(tokens, item) { onOpenReadme(item) }
                            }
                        }
                    }
                }
            }
            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = tokens.primary,
            )
        }
    }
}

@Composable
private fun TopBar(tokens: Tokens, title: String, showBack: Boolean, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(tokens.actionBarHeight).background(tokens.backgroundBottom),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            val ctx = LocalContext.current
            val backId = remember {
                ctx.drawableId("abc_ic_ab_back_material").takeIf { it != 0 }
                    ?: ctx.drawableId("abc_ic_ab_back_material", "androidx.appcompat")
            }
            Box(
                Modifier.size(56.dp).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                if (backId != 0) {
                    Icon(
                        painter = painterResource(backId),
                        contentDescription = "返回",
                        tint = tokens.textPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Text("←", color = Color.White, fontSize = 22.sp)
                }
            }
            Spacer(Modifier.width(16.dp))
        } else {
            Spacer(Modifier.width(72.dp))
        }
        HostText(text = title, sp = 20f, color = tokens.textPrimary)
    }
}

@Composable
private fun SearchBar(
    tokens: Tokens,
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
            // 内置 EditText 在 420dpi 下为 69px（≈26.3dp），固定容器高度以对齐
            .height(26.3.dp)
            .background(Color(0x54FFFFFF), RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val searchCtx = LocalContext.current
        val searchIconId = remember(searchCtx) { searchCtx.drawableId("ic_baseline_search_24") }
        if (searchIconId != 0) {
            Image(
                painter = painterResource(searchIconId),
                contentDescription = null,
                colorFilter = ColorFilter.tint(tokens.primary),
                modifier = Modifier.size(24.dp).padding(2.dp),
            )
        } else {
            Canvas(Modifier.size(24.dp).padding(2.dp)) {
                val stroke = 2.dp.toPx()
                drawCircle(
                    color = tokens.primary,
                    radius = size.minDimension * 0.3f,
                    center = Offset(size.width * 0.42f, size.height * 0.42f),
                    style = Stroke(width = stroke),
                )
                drawLine(
                    color = tokens.primary,
                    start = Offset(size.width * 0.63f, size.height * 0.63f),
                    end = Offset(size.width * 0.88f, size.height * 0.88f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        val ctx = LocalContext.current
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { appCtx ->
                EditText(appCtx).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setTextColor(tokens.textPrimary.toArgbCompat())
                    setHintTextColor(tokens.textSecondary.toArgbCompat())
                    hint = appCtx.hostString("course_resource_hint", "输入课程名、课程代码或教师名")
                    background = null
                    isSingleLine = true
                    inputType = android.text.InputType.TYPE_CLASS_TEXT
                    imeOptions = EditorInfo.IME_ACTION_SEARCH
                    setPadding(0, 0, 0, 0)
                    setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                            onSearch()
                            true
                        } else false
                    }
                    addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                        override fun afterTextChanged(s: Editable?) {
                            onValueChange(s?.toString().orEmpty())
                        }
                    })
                }
            },
            update = { view ->
                val current = view.text?.toString().orEmpty()
                if (current != value) {
                    view.setText(value)
                    view.setSelection(view.text.length)
                }
                view.setTextColor(tokens.textPrimary.toArgbCompat())
                view.setHintTextColor(tokens.textSecondary.toArgbCompat())
            },
        )
    }
}

@Composable
private fun CenterHint(tokens: Tokens, text: String) {
    Text(
        text,
        color = tokens.textSecondary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(24.dp),
    )
}

private val ResourceDecelerate = Easing { t -> 1f - (1f - t) * (1f - t) }

/** 复刻内置 RecyclerView layoutAnimation：400ms、延迟 20%、上移 20% + 淡入 + 105% 缩放。 */
@Composable
private fun EntranceAnimation(
    index: Int,
    generation: String,
    played: MutableMap<Int, Boolean>,
    content: @Composable BoxScope.() -> Unit,
) {
    val alreadyPlayed = played[index] == true
    if (!alreadyPlayed) played[index] = true

    val context = LocalContext.current
    // 跟随系统「动画程序时长缩放」，与内置 View 动画一致
    val animationScale = remember {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ).coerceIn(0f, 20f)
    }
    val durationMs = (400 * animationScale).toInt().coerceAtLeast(1)
    var started by remember(generation, index) { mutableStateOf(alreadyPlayed) }
    LaunchedEffect(generation, index) {
        if (!alreadyPlayed) {
            delay((index.coerceAtMost(10) * (80 * animationScale)).toLong())
            started = true
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = durationMs, easing = ResourceDecelerate),
        label = "entrance",
    )
    Box(
        Modifier.graphicsLayer {
            alpha = progress
            val scale = 1.05f - 0.05f * progress
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0.5f, 0.5f)
            translationY = -(1f - progress) * size.height * 0.2f
        },
        content = content,
    )
}

@Composable
private fun CourseCard(tokens: Tokens, item: CourseResourceItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = tokens.backgroundSecond,
        elevation = 0.dp,
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            val ctx = LocalContext.current
            val menuId = remember { ctx.drawableId("ic_baseline_menu_24") }
            if (menuId != 0) {
                Image(
                    painter = painterResource(menuId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(46.dp),
                )
            } else {
                MenuGlyph(tokens, Modifier.size(46.dp))
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp)) {
                HostText(
                    text = item.courseName.ifBlank { item.courseCode.ifBlank { item.repoName } },
                    sp = 16f,
                    color = tokens.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    buildString {
                        append(item.courseCode)
                        val teachers = item.teachers.take(3).joinToString(" / ")
                        if (teachers.isNotBlank()) {
                            if (isNotEmpty()) append("  ·  ")
                            append(teachers)
                        }
                    },
                    color = tokens.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.height(8.dp))
                TagView(
                    text = if (item.repoType == "multi-project") "多课程" else "课程",
                    tokens = tokens,
                )
            }
        }
    }
}

/** 用宿主 TextView 渲染文本，字形/抗锯齿与内置页面完全一致。 */
@Composable
private fun HostText(
    text: String,
    sp: Float,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    typefaceName: String? = null,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
                includeFontPadding = true
                setTextColor(color.toArgbCompat())
                typefaceName?.let { typeface = android.graphics.Typeface.create(it, android.graphics.Typeface.NORMAL) }
                this.maxLines = maxLines
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
        },
        update = {
            it.text = text
            it.maxLines = maxLines
        },
    )
}

@Composable
private fun TagView(text: String, tokens: Tokens) {
    val context = LocalContext.current
    val bgId = remember { context.drawableId("element_rounded_button_bg_grey") }
    val density = context.resources.displayMetrics.density
    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(tokens.textSecondary.toArgbCompat())
                includeFontPadding = true
                setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
                // 与内置 tag 的 70px 高度对齐（26.7dp）
                minHeight = (26.7f * density).toInt()
                if (bgId != 0) setBackgroundResource(bgId)
            }
        },
        update = { it.text = text },
    )
}

@Composable
private fun MenuGlyph(tokens: Tokens, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = 2.dp.toPx()
        val left = size.width * 0.28f
        val right = size.width * 0.72f
        listOf(0.35f, 0.5f, 0.65f).forEach { y ->
            drawLine(
                color = tokens.textPrimary,
                start = Offset(left, size.height * y),
                end = Offset(right, size.height * y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun ReadmeScreen(
    service: ResourceDataService?,
    hostForContribution: HostUiService?,
    tokens: Tokens,
    item: CourseResourceItem,
) {
    val repoName = item.repoName
    val empty = remember { MutableLiveData<DataState<CourseReadmeData>>() }
    val live = remember(repoName, service) { service?.getCourseReadme(repoName) }
    val result by (live ?: empty).observeAsState()
    val context = LocalContext.current
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(JLatexMathPlugin.create(context.resources.displayMetrics.density * 15f))
            .usePlugin(GlideImagesPlugin.create(context))
            .usePlugin(HtmlPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .build()
    }

    Column(Modifier.fillMaxSize().background(tokens.backgroundBottom)) {
        val title = item.courseName.ifBlank { repoName }
        TopBar(tokens, title = title, showBack = true, onBack = {
            (context as? androidx.activity.ComponentActivity)?.onBackPressedDispatcher?.onBackPressed()
        })

        Column(
            Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
        ) {
            Text(item.courseCode, color = tokens.textSecondary, fontSize = 14.sp)
            if (result?.state == DataState.STATE.SUCCESS) {
                Text(
                    "来源：${result?.data?.source.orEmpty()}",
                    color = tokens.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            val hostUi = remember { hostForContribution }
            OutlinedButton(
                onClick = {
                    hostUi?.openCourseContribution(
                        context,
                        repoName,
                        item.courseName,
                        item.courseCode,
                        item.repoType,
                    )
                },
                modifier = Modifier.padding(top = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = tokens.primary),
                border = BorderStroke(1.dp, tokens.primary),
            ) {
                Text("贡献课程资料")
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                service == null -> CenterHint(tokens, LocalContext.current.hostString("course_resource_failed", "资源服务不可用"))
                result == null -> Column(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text("正在加载 README…", color = tokens.textSecondary, fontSize = 14.sp)
                }
                result?.state != DataState.STATE.SUCCESS ->
                    CenterHint(tokens, "README 加载失败：${result?.message ?: result?.state}")
                result?.data == null -> CenterHint(tokens, "README 不存在")
                else -> AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    factory = { ctx ->
                        TextView(ctx).apply {
                            setTextIsSelectable(true)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                            setTextColor(tokens.textPrimary.toArgbCompat())
                        }
                    },
                    update = { view ->
                        view.setTextColor(tokens.textPrimary.toArgbCompat())
                        markwon.setMarkdown(view, result?.data?.markdown.orEmpty())
                    },
                )
            }
        }
    }
}

private fun Color.toArgbCompat(): Int = android.graphics.Color.argb(
    (alpha * 255 + 0.5f).toInt(),
    (red * 255 + 0.5f).toInt(),
    (green * 255 + 0.5f).toInt(),
    (blue * 255 + 0.5f).toInt(),
)
