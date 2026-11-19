package com.stupidtree.hitax.shell

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stupidtree.hitax.R
import com.stupidtree.hitax.market.BuiltinTabIcons
import com.stupidtree.hitax.market.PresetLayoutStore
import com.stupidtree.hitax.market.PresetStateStore
import com.stupidtree.hitax.market.PresetTab
import kotlin.math.roundToInt

/** 编辑当前整合包底栏；保存后由调用方重建 Activity 读入新布局。 */
@Composable
fun ShellTabEditor(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val presetId = PresetStateStore.activeId(context).orEmpty()
    val initial = remember {
        PresetLayoutStore.tabs(context).sortedBy { it.order }.ifEmpty { defaultPresetTabs() }
    }
    var tabs by remember { mutableStateOf(initial) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { 64.dp.toPx() }
    val icons = remember { BuiltinTabIcons.available().toList() }

    fun update(id: String, transform: (PresetTab) -> PresetTab) {
        tabs = tabs.map { if (it.id == id) transform(it) else it }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("编辑底栏", style = MaterialTheme.typography.titleLarge)
                Text(
                    "拖动左侧手柄排序；可改名、换图标或隐藏。更多入口不可隐藏。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                tabs.forEachIndexed { index, tab ->
                    val dragging = draggingId == tab.id
                    Row(
                        modifier = Modifier.fillMaxWidth().height(64.dp)
                            .graphicsLayer {
                                translationY = if (dragging) dragOffset else 0f
                            }
                            .alpha(if (dragging) 0.85f else 1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.width(36.dp).fillMaxHeight()
                                .pointerInput(tab.id) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggingId = tab.id
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y
                                        },
                                        onDragEnd = {
                                            val from = tabs.indexOfFirst { it.id == tab.id }
                                            if (from >= 0) {
                                                val to = (from + (dragOffset / rowHeightPx).roundToInt())
                                                    .coerceIn(0, tabs.lastIndex)
                                                if (to != from) {
                                                    val next = tabs.toMutableList()
                                                    next.add(to, next.removeAt(from))
                                                    tabs = next.mapIndexed { index, item ->
                                                        item.copy(order = index)
                                                    }
                                                }
                                            }
                                            draggingId = null
                                            dragOffset = 0f
                                        },
                                        onDragCancel = {
                                            draggingId = null
                                            dragOffset = 0f
                                        },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "拖拽排序")
                        }
                        IconButton(onClick = {
                            val current = BuiltinTabIcons.parseName(tab.iconAsset)
                            val at = icons.indexOf(current).let { if (it < 0) 0 else it }
                            val next = icons[(at + 1) % icons.size.coerceAtLeast(1)]
                            update(tab.id) { it.copy(iconAsset = "${BuiltinTabIcons.PREFIX}$next") }
                        }) {
                            Icon(
                                painter = painterResource(
                                    BuiltinTabIcons.resolve(tab.iconAsset) ?: R.drawable.ic_nav_plugin,
                                ),
                                contentDescription = "更换图标",
                            )
                        }
                        OutlinedTextField(
                            value = tab.title.orEmpty(),
                            onValueChange = { value ->
                                update(tab.id) { it.copy(title = value) }
                            },
                            singleLine = true,
                            label = { Text("名称") },
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = tab.visible != false,
                            onCheckedChange = { visible ->
                                if (tab.id != "hita.more") {
                                    update(tab.id) { it.copy(visible = visible) }
                                }
                            },
                            enabled = tab.id != "hita.more",
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(onClick = {
                        PresetLayoutStore.save(
                            context,
                            presetId,
                            tabs.mapIndexed { index, item -> item.copy(order = index) },
                        )
                        onSaved()
                    }) { Text("保存") }
                }
            }
        }
    }
}

private fun defaultPresetTabs(): List<PresetTab> = listOf(
    PresetTab("com.stupidtree.hita.ui.homework", "待办", order = 0, iconAsset = "builtin:check"),
    PresetTab("hita.timeline", "今日", order = 1, iconAsset = "builtin:today"),
    PresetTab("hita.timetable", "时间表", order = 2, iconAsset = "builtin:timetable"),
    PresetTab("com.stupidtree.hita.ui.ai", "助手", order = 3, iconAsset = "builtin:toys"),
    PresetTab("com.stupidtree.hita.ui.news", "资讯", order = 4, iconAsset = "builtin:blog"),
    PresetTab("hita.more", "更多", order = 5, iconAsset = "builtin:navigation", required = true),
)
