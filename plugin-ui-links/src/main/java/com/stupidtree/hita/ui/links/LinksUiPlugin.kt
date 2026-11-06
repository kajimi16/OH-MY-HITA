package com.stupidtree.hita.ui.links

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.stupidtree.hita.api.ComposeUiPlugin
import com.stupidtree.hita.api.PluginUiHost
import com.stupidtree.hita.api.ThemeController

class LinksUiPlugin : ComposeUiPlugin {
    override val id = "com.stupidtree.hita.ui.links"
    override val displayName = "实用网址"
    override val requiresApi = 7

    override fun createView(context: Context, host: PluginUiHost): View {
        return ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                            .padding(16.dp),
                    ) {
                        Text("实用网址", style = MaterialTheme.typography.h5)
                        Text("点按打开，长按由系统复制链接。", style = MaterialTheme.typography.caption)

                        val theme = remember { host.service(ThemeController.KEY) }
                        var preset by remember { mutableStateOf(theme?.colorPreset() ?: "classic") }
                        var fontScale by remember { mutableStateOf(theme?.fontScale() ?: 1f) }
                        Text("配色预设", style = MaterialTheme.typography.subtitle2)
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
                        Text("字体缩放 ${"%.2f".format(fontScale)}", style = MaterialTheme.typography.subtitle2)
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

                        links.forEach { (name, url) ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                                    .clickable {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        }
                                    },
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(name, style = MaterialTheme.typography.subtitle1)
                                    Text(url, style = MaterialTheme.typography.caption)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private companion object {
        val links = listOf(
            "HOA 课程资料" to "https://hoa.moe/",
            "哈工大（深圳）教务" to "https://jw.hitsz.edu.cn/",
            "哈工大本部教务" to "https://jwes.hit.edu.cn/",
            "哈工大（威海）教务" to "https://jw.hitwh.edu.cn/",
            "校园网自助服务" to "https://net.hitsz.edu.cn/",
            "图书馆" to "https://lib.hitsz.edu.cn/",
        )
    }
}
