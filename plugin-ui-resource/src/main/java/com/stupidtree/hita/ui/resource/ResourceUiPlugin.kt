package com.stupidtree.hita.ui.resource

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.MutableLiveData
import com.stupidtree.component.data.DataState
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.ResourceDataService
import com.stupidtree.hitax.data.model.resource.CourseReadmeData
import com.stupidtree.hitax.data.model.resource.CourseResourceItem
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin

/**
 * 课程资源（运行时界面插件）。
 *
 * 数据全部来自宿主的 [ResourceDataService]；本插件只负责渲染搜索列表与 README。
 * 投稿表单暂由内置页面承担（宿主只把“查询”路由到这里）。
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

@Composable
private fun ResourceApp(host: PluginUiHost, initialQuery: String) {
    val service = remember { host.service(ResourceDataService.KEY) }
    var repoName by remember { mutableStateOf<String?>(null) }
    // 输入与已提交的关键词提升到外层，返回列表时保持搜索状态
    var input by remember { mutableStateOf(initialQuery) }
    var submitted by remember { mutableStateOf(initialQuery) }

    BackHandler(enabled = repoName != null) { repoName = null }

    if (repoName == null) {
        SearchScreen(
            service = service,
            input = input,
            submitted = submitted,
            onInputChange = { input = it },
            onSubmit = { submitted = it },
            onOpenReadme = { repoName = it },
        )
    } else {
        ReadmeScreen(service = service, repoName = repoName!!)
    }
}

@Composable
private fun SearchScreen(
    service: ResourceDataService?,
    input: String,
    submitted: String,
    onInputChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onOpenReadme: (String) -> Unit,
) {
    val empty = remember { MutableLiveData<DataState<List<CourseResourceItem>>>() }
    val live = remember(submitted, service) {
        if (service == null || submitted.isBlank()) null else service.searchCourses(submitted)
    }
    val result by (live ?: empty).observeAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("课程名 / 代码 / 教师") },
            )
            Spacer(Modifier.padding(4.dp))
            Button(onClick = { onSubmit(input.trim()) }) { Text("搜索") }
        }
        Spacer(Modifier.height(12.dp))

        when {
            service == null -> Text("资源服务不可用")
            submitted.isBlank() -> Text("输入关键词后搜索课程资源")
            result == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.height(20.dp))
                Spacer(Modifier.padding(6.dp))
                Text("正在加载…")
            }
            result?.state != DataState.STATE.SUCCESS ->
                Text("课程资源加载失败：${result?.message ?: result?.state}")
            result?.data.isNullOrEmpty() -> Text("没有搜索到结果")
            else -> LazyColumn {
                items(result?.data ?: emptyList()) { item ->
                    CourseRow(item) { onOpenReadme(item.repoName) }
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun CourseRow(item: CourseResourceItem, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.clickable(onClick = onClick).padding(12.dp)) {
            Text(
                item.courseName.ifBlank { item.courseCode.ifBlank { item.repoName } },
                style = MaterialTheme.typography.subtitle1,
            )
            if (item.courseCode.isNotBlank()) {
                Text(item.courseCode, style = MaterialTheme.typography.caption)
            }
            if (item.teachers.isNotEmpty()) {
                Text(item.teachers.joinToString(" / "), style = MaterialTheme.typography.caption)
            }
            Text(
                if (item.repoType == "multi-project") "多课程" else "课程",
                style = MaterialTheme.typography.caption,
            )
        }
    }
}

@Composable
private fun ReadmeScreen(service: ResourceDataService?, repoName: String) {
    val empty = remember { MutableLiveData<DataState<CourseReadmeData>>() }
    val live = remember(repoName, service) { service?.getCourseReadme(repoName) }
    val result by (live ?: empty).observeAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(JLatexMathPlugin.create(context.resources.displayMetrics.density * 16f))
            .usePlugin(GlideImagesPlugin.create(context))
            .usePlugin(HtmlPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .build()
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("‹ 返回", Modifier.clickable { (context as? androidx.activity.ComponentActivity)?.onBackPressedDispatcher?.onBackPressed() })
            Spacer(Modifier.padding(8.dp))
            Text(repoName, style = MaterialTheme.typography.subtitle1)
        }
        Divider()
        when {
            service == null -> Text("资源服务不可用", Modifier.padding(16.dp))
            result == null -> Text("正在加载 README…", Modifier.padding(16.dp))
            result?.state != DataState.STATE.SUCCESS ->
                Text("README 加载失败：${result?.message ?: result?.state}", Modifier.padding(16.dp))
            result?.data == null -> Text("README 不存在", Modifier.padding(16.dp))
            else -> AndroidView(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                factory = { ctx -> TextView(ctx).apply { setTextIsSelectable(true) } },
                update = { view -> markwon.setMarkdown(view, result?.data?.markdown.orEmpty()) },
            )
        }
    }
}
