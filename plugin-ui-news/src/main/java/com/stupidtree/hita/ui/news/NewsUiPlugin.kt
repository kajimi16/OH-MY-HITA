package com.stupidtree.hita.ui.news

import android.content.Context
import android.text.Html
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class NewsUiPlugin : ComposeUiPlugin {
    override val id = "com.stupidtree.hita.ui.news"
    override val displayName = "资讯"
    override val requiresApi = 7

    override fun createView(context: Context, host: PluginUiHost): View {
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    var articles by remember { mutableStateOf<List<Article>>(emptyList()) }
                    var loading by remember { mutableStateOf(true) }
                    var error by remember { mutableStateOf<String?>(null) }
                    var reload by remember { mutableStateOf(0) }
                    var selected by remember { mutableStateOf<Article?>(null) }

                    LaunchedEffect(reload) {
                        loading = true
                        error = null
                        runCatching { withContext(Dispatchers.IO) { fetchFeed() } }
                            .onSuccess { articles = it }
                            .onFailure { error = it.message ?: "加载失败" }
                        loading = false
                    }

                    val article = selected
                    if (article != null) {
                        Column(Modifier.fillMaxSize()) {
                            Row(Modifier.fillMaxWidth().padding(8.dp)) {
                                Button(onClick = { selected = null }) { Text("返回") }
                                Text(
                                    article.title,
                                    style = MaterialTheme.typography.subtitle1,
                                    maxLines = 2,
                                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                                )
                            }
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        webViewClient = WebViewClient()
                                        loadUrl(article.url)
                                    }
                                },
                                update = { view ->
                                    if (view.url != article.url) view.loadUrl(article.url)
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    } else {
                        Column(Modifier.fillMaxSize().padding(16.dp)) {
                            Row {
                                Text("HOA 资讯", style = MaterialTheme.typography.h5,
                                    modifier = Modifier.weight(1f))
                                Button(onClick = { reload++ }) { Text("刷新") }
                            }
                            Text("来源：hoa.moe/blog", style = MaterialTheme.typography.caption)

                            when {
                                loading -> CircularProgressIndicator(Modifier.padding(top = 24.dp))
                                error != null -> Text("加载失败：$error")
                                articles.isEmpty() -> Text("暂无文章")
                                else -> LazyColumn(Modifier.padding(top = 8.dp)) {
                                    items(articles) { article ->
                                        Card(
                                            Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                                .clickable { selected = article },
                                        ) {
                                            Column(Modifier.padding(12.dp)) {
                                                Text(article.title, style = MaterialTheme.typography.subtitle1)
                                                Text(article.date, style = MaterialTheme.typography.caption)
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

    private fun fetchFeed(): List<Article> {
        val conn = URL(FEED_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", "OH-MY-HITA-News/0.1")
        return try {
            val xml = conn.inputStream.bufferedReader().use { it.readText() }
            parseItems(xml)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseItems(xml: String): List<Article> =
        Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL).findAll(xml).mapNotNull { item ->
            val body = item.groupValues[1]
            val title = Regex("<title>(?:<!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?</title>", RegexOption.DOT_MATCHES_ALL)
                .find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            val link = Regex("<link>(?:<!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?</link>", RegexOption.DOT_MATCHES_ALL)
                .find(body)?.groupValues?.get(1)?.trim().orEmpty()
            val date = Regex("<pubDate>(.*?)</pubDate>", RegexOption.DOT_MATCHES_ALL)
                .find(body)?.groupValues?.get(1)?.trim().orEmpty()
            Article(@Suppress("DEPRECATION") Html.fromHtml(title).toString(), link, date)
        }.take(30).toList()

    private data class Article(val title: String, val url: String, val date: String)

    private companion object {
        const val FEED_URL = "https://hoa.moe/blog/rss.xml"
    }
}
