package com.stupidtree.hitax.shell.agent

import android.content.Context
import android.text.method.LinkMovementMethod
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.stupidtree.hita.api.ResourceUiService
import com.stupidtree.hitax.HApplication
import com.stupidtree.hitax.agent.AgentChatViewModel
import com.stupidtree.hitax.agent.attachment.ChatAttachmentInput
import com.stupidtree.hitax.agent.attachment.ChatAttachmentProcessor
import com.stupidtree.hitax.agent.attachment.ChatAttachmentResult
import com.stupidtree.hitax.agent.attachment.DocumentTextParser
import com.stupidtree.hitax.agent.AgentMessage
import com.stupidtree.hitax.agent.model.AgentResourceCard
import com.stupidtree.hitax.agent.settings.AiSettings
import com.stupidtree.hitax.agent.settings.AiSettingsRepository
import com.stupidtree.hitax.agent.settings.AiChatProvider
import io.noties.markwon.Markwon
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import kotlinx.coroutines.delay

private val suggestions = listOf(
    "今天有什么课",
    "明天的课",
    "下一节课是什么",
    "现在第几周",
    "本周课表",
    "最近的考试",
)

@Composable
fun AgentChatScreen(viewModel: AgentChatViewModel, timetableId: String?) {
    val sessions by viewModel.sessions.observeAsState(emptyList())
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val markwon = remember(context) {
        Markwon.builder(context)
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(JLatexMathPlugin.create(13f))
            .build()
    }
    var input by remember { mutableStateOf("") }
    var sessionMenuExpanded by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<com.stupidtree.hitax.agent.model.ChatSession?>(null) }

    LaunchedEffect(viewModel.messages.size, viewModel.busy) {
        val target = viewModel.messages.lastIndex + if (viewModel.busy) 1 else 0
        if (target >= 0) listState.animateScrollToItem(target)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(top = 12.5.dp)) {
            Box(Modifier.fillMaxWidth().height(73.dp).padding(start = 16.dp), contentAlignment = Alignment.TopStart) {
                Text(
                    "助手",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(0.4.dp))
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, top = 0.dp, end = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    val title = sessions.firstOrNull { it.id == viewModel.currentSessionId }?.title
                        ?: sessions.firstOrNull()?.title
                        ?: "新对话"
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp).clickable { sessionMenuExpanded = true },
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                title,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    DropdownMenu(expanded = sessionMenuExpanded, onDismissRequest = { sessionMenuExpanded = false }) {
                        sessions.forEach { session ->
                            DropdownMenuItem(
                                text = { Text(session.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = {
                                    sessionMenuExpanded = false
                                    viewModel.switchToSession(session.id)
                                },
                            )
                        }
                    }
                }
                IconButton(onClick = { viewModel.createNewSession() }) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = {
                    deleteTarget = sessions.firstOrNull { it.id == viewModel.currentSessionId }
                }) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.MenuBook, "AI 设置", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            val visible = viewModel.messages
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 88.dp),
            ) {
                if (visible.isEmpty() && !viewModel.busy) {
                    item("agent-welcome-card") { AgentWelcomeCard { text -> viewModel.send(text, timetableId) } }
                } else {
                    itemsIndexed(visible, key = { index, item -> "${item.timestampMs}-${item.role}-$index" }) { _, message ->
                        AgentMessageBubble(message, markwon)
                    }
                }
                if (viewModel.busy) {
                    item("agent-placeholder") { ThinkingBubble() }
                }
            }
        }

        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val attachmentProcessor = remember {
            ChatAttachmentProcessor(
                directory = File(context.cacheDir, "chat-attachments").apply { mkdirs() },
                parser = DocumentTextParser(),
                encodeMedia = { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) },
            )
        }
        val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    val name = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                        ?: uri.lastPathSegment.orEmpty().substringAfterLast('/')
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            attachmentProcessor.process(object : ChatAttachmentInput {
                                override val displayName = name
                                override fun open() = context.contentResolver.openInputStream(uri)
                                    ?: throw IOException("无法打开文件")
                            })
                        }.getOrElse { ChatAttachmentResult.Failure(ChatAttachmentResult.Kind.READ, name, it.message) }
                    }
                    when (result) {
                        is ChatAttachmentResult.Text -> {
                            if (result.content.isBlank()) {
                                Toast.makeText(context, "附件内容为空", Toast.LENGTH_SHORT).show()
                            } else {
                                input = buildString {
                                    if (input.isNotBlank()) append(input).append("\n")
                                    append("[附件: ").append(result.fileName).append("]\n").append(result.content)
                                }
                            }
                        }
                        is ChatAttachmentResult.Media -> {
                            val prompt = input.trim().ifEmpty { "请分析这个附件" }
                            input = ""
                            viewModel.sendMedia(prompt, result, timetableId)
                        }
                        is ChatAttachmentResult.Failure -> {
                            val message = when (result.kind) {
                                ChatAttachmentResult.Kind.TOO_LARGE -> "附件过大（上限 ${result.maxBytes / 1024 / 1024}MB）"
                                ChatAttachmentResult.Kind.UNSUPPORTED -> "不支持的附件类型"
                                ChatAttachmentResult.Kind.EMPTY -> "附件为空"
                                ChatAttachmentResult.Kind.PARSE -> "附件解析失败"
                                ChatAttachmentResult.Kind.EXPANDED_LIMIT -> "Office 文件展开后过大"
                                ChatAttachmentResult.Kind.MEMORY -> "附件过大，内存不足"
                                ChatAttachmentResult.Kind.READ -> "附件读取失败"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .padding(start = 12.dp, end = 12.dp, bottom = 9.7.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            IconButton(
                onClick = {
                    attachmentPicker.launch(arrayOf("*/*"))
                },
                enabled = !viewModel.busy,
                modifier = Modifier.size(44.dp).padding(end = 4.dp),
            ) {
                Icon(Icons.Default.Add, "添加文本附件", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextField(
                value = input,
                onValueChange = { input = it },
                enabled = !viewModel.busy,
                placeholder = { Text("输入消息…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                shape = RoundedCornerShape(22.dp),
                minLines = 1,
                maxLines = 8,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    val text = input
                    input = ""
                    keyboard?.hide()
                    viewModel.send(text, timetableId)
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.weight(1f).heightIn(min = 52.dp, max = 156.dp),
            )
            IconButton(
                onClick = {
                    val text = input
                    input = ""
                    keyboard?.hide()
                    viewModel.send(text, timetableId)
                },
                enabled = !viewModel.busy && input.isNotBlank(),
                modifier = Modifier.padding(start = 8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            ) {
                if (viewModel.busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.KeyboardArrowRight, "发送", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }

    if (showSettings) {
        AiSettingsDialog(onDismiss = { showSettings = false })
    }
    deleteTarget?.let { session ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除会话") },
            text = { Text("删除「${session.title}」的聊天记录？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSession(session)
                    deleteTarget = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgentWelcomeCard(onSend: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth(0.9f),
        ) {
            Text(
                "你好，我是 HITA 助手。可以问我课表相关的问题，比如“明天有什么课”“第三周周五的课”“下一节课在哪”“军事理论什么时候上”。\n在右上角设置中填写 DeepSeek 或智谱的 API Key 后，还能回答更开放的校园问题。",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(12.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(
            Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestions.forEach { suggestion ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.clickable { onSend(suggestion) },
                ) {
                    Text(suggestion, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun AgentMessageBubble(message: AgentMessage, markwon: Markwon) {
    val isUser = message.role == "user"
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth(0.88f),
        ) {
            if (isUser) {
                Text(
                    message.text,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(12.dp),
                )
            } else {
                Column {
                    MarkdownMessageText(message.text, markwon, MaterialTheme.colorScheme.onSurface)
                    if (message.cards.isNotEmpty()) {
                        AgentResourceCards(message.cards)
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentResourceCards(cards: List<AgentResourceCard>) {
    val context = LocalContext.current
    Column(
        Modifier.padding(start = 12.dp, end = 12.dp, bottom = 9.7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        cards.forEach { card ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().clickable { openResourceCard(context, card) },
            ) {
                Column(Modifier.padding(10.dp)) {
                    Text(
                        card.title.ifBlank { card.path },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (card.subtitle.isNotBlank()) {
                        Text(
                            card.subtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun openResourceCard(context: Context, card: AgentResourceCard) {
    if (card.sourceTag != "HOA" && card.source != "HOA") return
    val host = context.applicationContext as? HApplication ?: return
    val service = host.pluginHost.service(ResourceUiService.KEY) ?: return
    val owner = context as? androidx.lifecycle.LifecycleOwner ?: return
    service.openReadme(
        context,
        owner,
        card.courseCode.takeIf { it.isNotBlank() },
        card.title.takeIf { it.isNotBlank() },
    )
}

@Composable
private fun ThinkingBubble() {
    var frame by remember { mutableIntStateOf(0) }
    val texts = listOf("正在思考", "正在思考.", "正在思考..", "正在思考...")
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            frame = (frame + 1) % texts.size
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Start) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(0.88f),
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    texts[frame],
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun MarkdownMessageText(markdown: String, markwon: Markwon, color: Color) {
    val textColor = color.toArgb()
    AndroidView(
        factory = { context ->
            TextView(context).apply {
                textSize = 14f
                setTextColor(textColor)
                movementMethod = LinkMovementMethod.getInstance()
                linksClickable = true
            }
        },
        update = { textView ->
            textView.setTextColor(textColor)
            markwon.setMarkdown(textView, markdown)
            textView.movementMethod = LinkMovementMethod.getInstance()
        },
        modifier = Modifier.padding(12.dp),
    )
}

@Composable
private fun AiSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { AiSettingsRepository(context) }
    val current = remember { repo.getSettings() }
    var deepSeek by remember { mutableStateOf(current.customDeepSeekApiKey) }
    var zhipu by remember { mutableStateOf(current.customZhipuApiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "此版本未配置内置 Key。请填写自定义 DeepSeek Key；它只保存在本机。",
                    fontSize = 13.sp,
                )
                OutlinedTextField(
                    value = deepSeek,
                    onValueChange = { deepSeek = it },
                    label = { Text("自定义 DeepSeek API Key") },
                    placeholder = { Text("sk-...") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    value = zhipu,
                    onValueChange = { zhipu = it },
                    label = { Text("自定义智谱 API Key（可留空）") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                repo.saveSettings(
                    AiSettings(
                        chatProvider = AiChatProvider.CUSTOM_DEEPSEEK,
                        customDeepSeekApiKey = deepSeek,
                        customZhipuApiKey = zhipu,
                    )
                )
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
