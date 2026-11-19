package com.stupidtree.hitax.shell.news

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.Html
import android.webkit.CookieManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

private data class BlogItem(val title: String, val url: String, val date: String, val millis: Long, val path: String)
private data class BlogNode(val item: BlogItem, val children: List<BlogNode> = emptyList())

/** HITA_Android 原版资讯页的 Shell 入口：HOA 资讯 + 校园通知两个分页。 */
@Composable
internal fun ShellNewsPage() {
    var tab by remember { mutableIntStateOf(0) }
    val dark = isSystemInDarkTheme()
    val pageBg = if (dark) MaterialTheme.colorScheme.background else Color(0xFFFAFBFE)
    Column(Modifier.fillMaxSize().background(pageBg)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, top = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("资讯", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(11.dp))
        TabRow(
            selectedTabIndex = tab,
            containerColor = if (dark) MaterialTheme.colorScheme.surface else Color.White,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("博客") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("校园通知") })
        }
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "news_tab",
        ) { page ->
            when (page) {
                0 -> BlogTab()
                else -> NoticeTab()
            }
        }
    }
}

@Composable
private fun BlogTab() {
    val context = LocalContext.current
    var articles by remember { mutableStateOf<List<BlogItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var expanded by remember { mutableStateOf<Set<String>>(emptySet()) }
    val blogFont = remember { FontFamily(Typeface.createFromAsset(context.assets, "blog/lxgw-wenkai.ttf")) }

    LaunchedEffect(reload) {
        loading = true
        error = null
        runCatching { withContext(Dispatchers.IO) { fetchBlog() } }
            .onSuccess { articles = it }
            .onFailure { error = it.message ?: "加载失败" }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> EmptyMessage("加载失败：$error") { reload++ }
            articles.isEmpty() -> EmptyMessage("暂无文章") { reload++ }
            else -> {
                val nodes = remember(articles) { groupBlog(articles) }
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(nodes, key = { it.item.url }) { node ->
                        val isSeries = node.children.isNotEmpty()
                        val isOpen = node.item.url in expanded
                        Surface(
                            Modifier.fillMaxWidth().clickable {
                                if (isSeries) {
                                    expanded = if (isOpen) expanded - node.item.url else expanded + node.item.url
                                } else {
                                    openUrl(context, node.item.url)
                                }
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else Color.White,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            shadowElevation = 10.dp,
                        ) {
                            Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 14.2.dp)) {
                                Row(Modifier.fillMaxWidth()) {
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(node.item.title, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, fontFamily = blogFont)
                                        Spacer(Modifier.height(2.dp))
                                        Text(node.item.date, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontFamily = blogFont)
                                    }
                                    if (isSeries) {
                                        Text("▸", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                                    }
                                }
                                if (isSeries && isOpen) {
                                    Column(Modifier.padding(top = 8.dp, start = 8.dp)) {
                                        node.children.forEach { child ->
                                            Row(
                                                Modifier.fillMaxWidth().clickable { openUrl(context, child.item.url) }
                                                    .padding(vertical = 6.dp),
                                            ) {
                                                Spacer(Modifier.width(10.dp))
                                                Column {
                                                    Text(child.item.title, fontWeight = FontWeight.Medium, fontSize = 15.sp, fontFamily = blogFont)
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(child.item.date, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontFamily = blogFont)
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
        }
    }
}

@Composable
private fun NoticeTab() {
    val context = LocalContext.current
    var cookie by remember { mutableStateOf(readCookie()) }
    val loginLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        CookieManager.getInstance().flush()
        cookie = readCookie()
    }
    var notices by remember { mutableStateOf<List<CampusNotice>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(cookie, reload) {
        if (cookie.isBlank()) return@LaunchedEffect
        loading = true
        error = null
        runCatching { withContext(Dispatchers.IO) { NoticeFetcher.fetch(cookie) } }
            .onSuccess { notices = it }
            .onFailure { error = it.message ?: "加载失败" }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        if (cookie.isBlank()) {
            EmptyMessage("需要先登录 info.hitsz.edu.cn", "登录门户") {
                loginLauncher.launch(Intent(context, ShellPortalLoginActivity::class.java))
            }
            return@Column
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> EmptyMessage("加载失败：$error") { reload++ }
            notices.isEmpty() -> EmptyMessage("暂无通知") { reload++ }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(notices, key = { it.id }) { notice ->
                    Surface(
                        Modifier.fillMaxWidth().clickable { openUrl(context, notice.url) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else Color.White,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shadowElevation = 6.dp,
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(notice.title, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            if (notice.date.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(notice.date, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String, action: String = "重试", onAction: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onAction) { Text(action) }
    }
}

private fun openUrl(context: Context, url: String) {
    if (url.isBlank()) return
    context.startActivity(
        Intent(context, ShellNewsDetailActivity::class.java)
            .putExtra(ShellNewsDetailActivity.EXTRA_URL, url)
    )
}

private fun readCookie(): String = runCatching {
    CookieManager.getInstance().getCookie(NoticeParser.PORTAL).orEmpty()
}.getOrDefault("")

private fun fetchBlog(): List<BlogItem> {
    val conn = URL("https://hoa.moe/blog/rss.xml").openConnection() as HttpURLConnection
    conn.connectTimeout = 10_000
    conn.readTimeout = 20_000
    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36")
    conn.setRequestProperty("Accept", "application/rss+xml,application/xml;q=0.9,*/*;q=0.8")
    return try {
        val xml = conn.inputStream.bufferedReader().use { it.readText() }
        Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml)
            .mapNotNull { item ->
                val body = item.groupValues[1]
                val title = tag(body, "title")?.let { Html.fromHtml(it).toString() } ?: return@mapNotNull null
                val url = tag(body, "link") ?: return@mapNotNull null
                val rawDate = tag(body, "pubDate")
                BlogItem(title, url, formatDate(rawDate), parseDateMillis(rawDate), pathFromLink(url))
            }
            .sortedWith(compareByDescending<BlogItem> { it.millis }.thenByDescending { it.path })
            .take(30)
            .toList()
    } finally {
        conn.disconnect()
    }
}

private fun tag(body: String, name: String): String? =
    Regex("<$name>(?:<!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?</$name>", RegexOption.DOT_MATCHES_ALL)
        .find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

private fun parseDateMillis(raw: String?): Long {
    if (raw.isNullOrBlank()) return 0L
    val parser = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
    parser.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    return runCatching { parser.parse(raw)?.time ?: 0L }.getOrDefault(0L)
}

private fun pathFromLink(link: String): String =
    link.substringAfter("hoa.moe/", link).removePrefix("blog/").substringBefore('?').trim('/')

private fun groupBlog(articles: List<BlogItem>): List<BlogNode> {
    val byPath = articles.filter { it.path.isNotBlank() }.associateBy { it.path }
    val children = LinkedHashMap<String, MutableList<BlogItem>>()
    val roots = mutableListOf<BlogItem>()
    for (article in articles) {
        if (article.path.isBlank()) { roots += article; continue }
        var cursor = article.path.substringBeforeLast('/', "")
        var ancestor: String? = null
        while (cursor.isNotEmpty()) {
            if (cursor in byPath) { ancestor = cursor; break }
            cursor = cursor.substringBeforeLast('/', "")
        }
        if (ancestor == null) roots += article else children.getOrPut(ancestor) { mutableListOf() } += article
    }
    fun build(article: BlogItem): BlogNode = BlogNode(
        article,
        children[article.path].orEmpty().sortedWith(compareBy({ it.path }, { it.millis })).map(::build),
    )
    return roots.distinctBy { it.url }.sortedByDescending { it.millis }.map(::build)
}

private fun formatDate(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val parser = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
    parser.timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    val date = runCatching { parser.parse(raw) }.getOrNull() ?: return raw
    return SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).apply {
        timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }.format(date)
}
