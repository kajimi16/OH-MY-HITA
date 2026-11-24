package com.stupidtree.hitax.shell.agent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.stupidtree.hitax.agent.tools.ConfirmationRequest

/**
 * 写操作确认对话框；3.3.x 聊天页与视觉测试共用同一实现。
 */
@Composable
fun WriteConfirmationDialog(
    request: ConfirmationRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onReject,
        title = { Text(request.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                request.detailLines.forEach { line ->
                    Text(
                        text = line,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onApprove) { Text(request.confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onReject) { Text(request.cancelLabel) }
        },
    )
}
