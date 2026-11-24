package com.stupidtree.hita.plugin.todoui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.stupidtree.hita.api.TodoService
import kotlinx.coroutines.launch

/**
 * GeniusTA 账号密码登录。
 *
 * 前置条件：[service] 可用；密码只存在于本组合的局部状态。
 * 后置条件：成功回调 [onSuccess] 并清空密码；失败保留输入并显示错误。
 * 不变式：提交期间禁用重复提交与取消。
 */
@Composable
internal fun GeniusTaLoginDialog(
    service: TodoService,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (submitting) return
        if (username.isBlank() || password.isBlank()) {
            error = "账号与密码不能为空"
            return
        }
        submitting = true
        error = null
        scope.launch {
            val ok = try {
                service.loginGeniusTa(username.trim(), password)
            } finally {
                submitting = false
            }
            if (ok) {
                password = ""
                onSuccess()
            } else {
                error = "登录失败：请检查账号密码与网络，或查看 GeniusTA 账号状态。"
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("登录 GeniusTA") },
        text = {
            Column {
                Text(
                    "凭据只写入待办插件的私有加密存储，不进入界面状态之外的日志。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; error = null },
                    label = { Text("账号") },
                    singleLine = true,
                    enabled = !submitting,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = null },
                    label = { Text("密码") },
                    singleLine = true,
                    enabled = !submitting,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { submit() },
                enabled = !submitting && username.isNotBlank() && password.isNotBlank(),
            ) {
                Text(if (submitting) "登录中…" else "登录")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) { Text("取消") }
        },
    )
}

/**
 * Grader 会话登录：粘贴 Cookie 的兜底方式。
 *
 * 前置条件：cookie 文本包含唯一且非空的 sessionid，且全部为可打印 ASCII。
 * 后置条件：成功回调 [onSuccess]；失败显示原因并保留输入；点 [onOpenWebLogin] 切换到内嵌网页登录。
 * 不变式：本对话框不创建 WebView，不把 cookie 持久化到界面层。
 */
@Composable
internal fun GraderLoginDialog(
    service: TodoService,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onOpenWebLogin: () -> Unit,
) {
    var cookie by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (submitting) return
        val header = cookie.trim()
        val problem = validateGraderCookie(header)
        if (problem != null) {
            error = problem
            return
        }
        submitting = true
        error = null
        scope.launch {
            val ok = try {
                service.connectGrader(header)
            } finally {
                submitting = false
            }
            if (ok) {
                cookie = ""
                onSuccess()
            } else {
                error = "连接失败：会话无效、账号不可用或网络错误。"
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("连接 Grader") },
        text = {
            Column {
                Text(
                    "推荐使用内嵌网页登录：在插件内完成学校 CAS 登录后自动连接。",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = onOpenWebLogin, enabled = !submitting) {
                    Text("打开网页登录")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "可在浏览器打开 grader.hitsz.edu.cn 并完成 CAS 登录，\n" +
                        "从开发者工具的 Cookies 中复制完整 Cookie 头或 sessionid=... 形式粘贴到下方。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = cookie,
                    onValueChange = { cookie = it; error = null },
                    label = { Text("Cookie 头") },
                    placeholder = { Text("sessionid=...") },
                    minLines = 3,
                    maxLines = 5,
                    enabled = !submitting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }, enabled = !submitting && cookie.isNotBlank()) {
                Text(if (submitting) "连接中…" else "连接")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) { Text("取消") }
        },
    )
}

/** 返回首个格式问题；null 表示可交给 TodoService 校验。与 GraderSessionPolicy 规则一致。 */
internal fun validateGraderCookie(header: String): String? {
    if (header.isBlank()) return "请粘贴 Cookie"
    if (header.any { it.code < 0x20 || it.code > 0x7e }) return "Cookie 只能包含可打印 ASCII 字符"
    val matches = header.split(';').map { it.trim() }.filter { it.substringBefore('=') == "sessionid" }
    val value = matches.singleOrNull()?.substringAfter('=', "") ?: return "Cookie 必须且只能包含一个 sessionid"
    if (value.isEmpty()) return "sessionid 不能为空"
    if (value.any { it == '"' || it == ',' || it == ';' || it == '\\' || it == ' ' }) return "sessionid 含有非法字符"
    return null
}
