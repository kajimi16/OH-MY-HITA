package com.stupidtree.hita.ui.ai

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.TimetableService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class AiUiPlugin : ComposeUiPlugin {
    override val id = "com.stupidtree.hita.ui.ai"
    override val displayName = "AI 助手"
    override val requiresApi = 7

    override fun createView(context: Context, host: PluginUiHost): View {
        val prefs = context.getSharedPreferences("ai_plugin", Context.MODE_PRIVATE)
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    val scope = rememberCoroutineScope()
                    var endpoint by remember {
                        mutableStateOf(prefs.getString("endpoint", DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT)
                    }
                    var model by remember {
                        mutableStateOf(prefs.getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL)
                    }
                    var apiKey by remember { mutableStateOf(SecretStore.load(context)) }
                    var input by remember { mutableStateOf("") }
                    var busy by remember { mutableStateOf(false) }
                    var error by remember { mutableStateOf<String?>(null) }
                    val messages = remember { mutableStateListOf<Message>() }

                    fun save() {
                        prefs.edit()
                            .putString("endpoint", endpoint)
                            .putString("model", model)
                            .apply()
                        SecretStore.save(context, apiKey)
                    }

                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text("AI 助手", style = MaterialTheme.typography.h5)
                        Text("使用你自己的 OpenAI 兼容 API Key；对话仅保存在当前页面。",
                            style = MaterialTheme.typography.caption)

                        OutlinedTextField(
                            value = endpoint,
                            onValueChange = { endpoint = it },
                            label = { Text("接口地址") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Row {
                            OutlinedTextField(
                                value = model,
                                onValueChange = { model = it },
                                label = { Text("模型") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it },
                                label = { Text("API Key") },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true,
                            )
                        }

                        LazyColumn(Modifier.weight(1f).padding(vertical = 8.dp)) {
                            items(messages) { message ->
                                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                    Column(Modifier.padding(10.dp)) {
                                        Text(if (message.role == "user") "我" else "AI",
                                            style = MaterialTheme.typography.caption)
                                        Text(message.content)
                                    }
                                }
                            }
                        }
                        error?.let { Text("错误：$it", color = MaterialTheme.colors.error) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it },
                                label = { Text("输入消息") },
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                enabled = !busy && input.isNotBlank() && apiKey.isNotBlank(),
                                onClick = {
                                    val text = input.trim()
                                    input = ""
                                    error = null
                                    save()
                                    messages += Message("user", text)
                                    busy = true
                                    scope.launch {
                                        runCatching {
                                            withContext(Dispatchers.IO) {
                                                chat(
                                                    endpoint,
                                                    model,
                                                    apiKey,
                                                    messages.toList(),
                                                    todayContext(host),
                                                )
                                            }
                                        }.onSuccess { messages += Message("assistant", it) }
                                            .onFailure { error = it.message ?: "请求失败" }
                                        busy = false
                                    }
                                },
                            ) { Text(if (busy) "发送中…" else "发送") }
                        }
                    }
                }
            }
        }
    }

    private fun todayContext(host: PluginUiHost): String {
        val events = runCatching {
            host.service(TimetableService.KEY)?.getTodayEventsSync().orEmpty()
        }.getOrDefault(emptyList())
        if (events.isEmpty()) return "今日课表：无课程。"
        val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return "今日课表：\n" + events.joinToString("\n") { event ->
            "- ${fmt.format(java.util.Date(event.from.time))}-${fmt.format(java.util.Date(event.to.time))} " +
                "${event.name} @ ${event.place.orEmpty()}"
        }
    }

    private fun chat(
        endpoint: String,
        model: String,
        apiKey: String,
        messages: List<Message>,
        today: String,
    ): String {
        val history = JsonArray()
        fun addMessage(role: String, content: String) {
            history.add(JsonObject().apply {
                addProperty("role", role)
                addProperty("content", content)
            })
        }
        addMessage("system", SYSTEM_PROMPT + "\n" + today)
        messages.forEach { addMessage(it.role, it.content) }

        var response = post(endpoint, apiKey, requestBody(model, history, withTools = true))
        var choice = response.getAsJsonArray("choices")?.firstOrNull()?.asJsonObject
            ?.getAsJsonObject("message") ?: error("响应缺少 choices[0].message")
        val calls = choice.getAsJsonArray("tool_calls")
        if (calls != null && calls.size() > 0) {
            history.add(choice)
            calls.forEach { element ->
                val call = element.asJsonObject
                val id = call.get("id")?.asString ?: ""
                val name = call.getAsJsonObject("function")?.get("name")?.asString.orEmpty()
                val result = when (name) {
                    "get_today_courses" -> today
                    else -> "未知工具：$name"
                }
                history.add(JsonObject().apply {
                    addProperty("role", "tool")
                    addProperty("tool_call_id", id)
                    addProperty("content", result)
                })
            }
            response = post(endpoint, apiKey, requestBody(model, history, withTools = false))
            choice = response.getAsJsonArray("choices")?.firstOrNull()?.asJsonObject
                ?.getAsJsonObject("message") ?: error("响应缺少 choices[0].message")
        }
        return choice.get("content")?.asString ?: error("响应缺少文本内容")
    }

    private fun requestBody(model: String, history: JsonArray, withTools: Boolean): String {
        val root = JsonObject().apply {
            addProperty("model", model)
            add("messages", history)
            if (withTools) {
                add("tools", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("type", "function")
                        add("function", JsonObject().apply {
                            addProperty("name", "get_today_courses")
                            addProperty("description", "查询今天课程的时间、名称和地点")
                            add("parameters", JsonObject().apply {
                                addProperty("type", "object")
                                add("properties", JsonObject())
                            })
                        })
                    })
                })
            }
        }
        return com.google.gson.Gson().toJson(root)
    }

    private fun post(endpoint: String, apiKey: String, body: String): JsonObject {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 15_000
        conn.readTimeout = 60_000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer $apiKey")
        return try {
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("HTTP $code: ${text.take(300)}")
            JsonParser.parseString(text).asJsonObject
        } finally {
            conn.disconnect()
        }
    }

    private data class Message(val role: String, val content: String)

    private companion object {
        const val DEFAULT_ENDPOINT = "https://api.deepseek.com/chat/completions"
        const val DEFAULT_MODEL = "deepseek-chat"
        const val SYSTEM_PROMPT = "你是 HITA 校园助手，用简洁中文回答。"
    }
}
