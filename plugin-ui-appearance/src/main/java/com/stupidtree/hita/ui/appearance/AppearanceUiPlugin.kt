package com.stupidtree.hita.ui.appearance

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.ThemeController

class AppearanceUiPlugin : ComposeUiPlugin {
    override val id = "com.stupidtree.hita.ui.appearance"
    override val displayName = "外观与字体"
    override val requiresApi = 7

    override fun createView(context: Context, host: PluginUiHost): View {
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    val theme = remember { host.service(ThemeController.KEY) }
                    var preset by remember { mutableStateOf(theme?.colorPreset() ?: "classic") }
                    var fontScale by remember { mutableStateOf(theme?.fontScale() ?: 1f) }
                    var dark by remember { mutableStateOf(false) }
                    var style by remember { mutableStateOf(theme?.themeStyle() ?: "classic") }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text("外观与字体", style = MaterialTheme.typography.h5)
                        Text("配色与字体由宿主应用，重启当前界面后可见。", style = MaterialTheme.typography.body2)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("深色模式", modifier = Modifier.weight(1f))
                            Switch(checked = dark, onCheckedChange = {
                                dark = it
                                theme?.setNightMode(it)
                            })
                        }
                        Text("配色", style = MaterialTheme.typography.subtitle1)
                        Row {
                            listOf("classic", "fresh", "focus", "high_contrast").forEach { id ->
                                Button(
                                    onClick = {
                                        if (theme?.setColorPreset(id) == true) {
                                            preset = id
                                            (context as? android.app.Activity)?.recreate()
                                        }
                                    },
                                    modifier = Modifier.padding(end = 6.dp),
                                ) { Text(if (id == preset) "✓$id" else id) }
                            }
                        }
                        theme?.tokens()?.let { tokens ->
                            Text(
                                "组件参数：圆角 ${tokens.cornerRadiusDp.toInt()}dp · " +
                                    "间距 ${tokens.spacingDp.toInt()}dp · 玻璃 ${"%.2f".format(tokens.glassAlpha)}",
                                style = MaterialTheme.typography.caption,
                            )
                        }
                        Text("主题风格", style = MaterialTheme.typography.subtitle1)
                        listOf(
                            listOf("classic", "apple_glass", "cyber", "sora_cloud"),
                            listOf("p5", "deep_space", "sumi"),
                        ).forEach { row ->
                            Row {
                                row.forEach { id ->
                                    Button(
                                        onClick = {
                                            if (theme?.setThemeStyle(id) == true) {
                                                style = id
                                                (context as? android.app.Activity)?.recreate()
                                            }
                                        },
                                        modifier = Modifier.padding(end = 6.dp),
                                    ) { Text(if (id == style) "✓$id" else id) }
                                }
                            }
                        }
                        Text("字体缩放 ${"%.2f".format(fontScale)}", style = MaterialTheme.typography.subtitle1)
                        Row {
                            Button(onClick = {
                                val next = (fontScale - 0.05f).coerceAtLeast(0.85f)
                                if (theme?.setFontScale(next) == true) {
                                    fontScale = next
                                    (context as? android.app.Activity)?.recreate()
                                }
                            }) { Text("A-") }
                            Button(onClick = {
                                val next = (fontScale + 0.05f).coerceAtMost(1.30f)
                                if (theme?.setFontScale(next) == true) {
                                    fontScale = next
                                    (context as? android.app.Activity)?.recreate()
                                }
                            }) { Text("A+") }
                        }
                    }
                }
            }
        }
    }
}
