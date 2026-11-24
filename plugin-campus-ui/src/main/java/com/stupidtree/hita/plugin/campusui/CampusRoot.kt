package com.stupidtree.hita.plugin.campusui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stupidtree.hita.api.CampusService
import com.stupidtree.hita.api.CampusServiceTarget
import com.stupidtree.hita.api.PluginUiHost
import kotlinx.coroutines.launch

/** 四个门户；顺序与顶部标签一致。 */
internal enum class CampusTab(val title: String) {
    CARD("校园卡"),
    POD("静音仓"),
    LIBRARY("图书馆"),
    LAB("实验中心"),
}

/**
 * 插件主界面：顶部标签切换四个门户，登录覆盖层盖在最上层。
 *
 * 前置条件：宿主已注册 CampusService；界面运行在主线程。
 * 后置条件：登录成功后 refreshToken 自增，当前页据此重新查询。
 * 不变式：同一时刻最多一个登录覆盖层；关闭一律走 [PluginUiHost.close]。
 * 违约处理：service 缺失时入口显示不可用并仍可关闭；页面失败由各页自行展示。
 */
@Composable
internal fun CampusRoot(host: PluginUiHost, service: CampusService) {
    val target = remember(host) { host.arguments.getString("target") }
    val singlePage = target != null
    var tab by remember {
        mutableStateOf(
            when (target) {
                "pod" -> CampusTab.POD
                "library" -> CampusTab.LIBRARY
                "lab" -> CampusTab.LAB
                else -> CampusTab.CARD
            },
        )
    }
    var refreshToken by remember { mutableIntStateOf(0) }
    var loginTarget by remember { mutableStateOf<CampusServiceTarget?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun notify(message: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                if (!singlePage) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "校园",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(onClick = { host.close() }) { Text("关闭") }
                }
                TabRow(selectedTabIndex = tab.ordinal) {
                    CampusTab.entries.forEach { entry ->
                        Tab(
                            selected = entry == tab,
                            onClick = { tab = entry },
                            text = { Text(entry.title) },
                        )
                    }
                }
                }
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().weight(1f)) {
                    when (tab) {
                        CampusTab.CARD -> CardScreen(
                            service = service,
                            refreshToken = refreshToken,
                            onLoginRequired = { loginTarget = it },
                        )
                        CampusTab.POD -> PodScreen(
                            service = service,
                            refreshToken = refreshToken,
                            onLoginRequired = { loginTarget = it },
                            onMessage = ::notify,
                        )
                        CampusTab.LIBRARY -> LibraryScreen(
                            service = service,
                            refreshToken = refreshToken,
                            onLoginRequired = { loginTarget = it },
                        )
                        CampusTab.LAB -> LabScreen(
                            service = service,
                            refreshToken = refreshToken,
                            onLoginRequired = { loginTarget = it },
                        )
                    }
                }
            }
            loginTarget?.let { target ->
                CampusLoginOverlay(
                    service = service,
                    target = target,
                    onClose = { loginTarget = null },
                    onSignedIn = {
                        loginTarget = null
                        refreshToken += 1
                        notify("${targetLabel(target)}已登录，正在刷新")
                    },
                )
            }
        }
    }
}
