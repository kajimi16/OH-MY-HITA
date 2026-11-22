package com.stupidtree.hitax.onboarding

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider

/**
 * 首次启动引导：介绍页 + 整合包选择页。
 *
 * 前置条件：由 MainActivity 在 `preset_prompt_seen` 为 false 时启动。
 * 后置条件：向导完成（应用整合包或从零开始）时写入 seen 并结束；用户从介绍页返回时直接结束且不写 seen。
 * 不变式：应用整合包期间忽略返回键；页面状态只由本 Activity 的 UI 状态与 ViewModel 驱动。
 * 违约处理：状态已完成但 Activity 已结束时跳过写入。
 */
class PresetOnboardingActivity : AppCompatActivity() {

    private lateinit var viewModel: PresetOnboardingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel = ViewModelProvider(this)[PresetOnboardingViewModel::class.java]
        setContent {
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
            ) {
                val state by viewModel.state.collectAsState()
                var page by rememberSaveable { mutableIntStateOf(PAGE_INTRO) }

                LaunchedEffect(state.completed) {
                    if (state.completed && !isFinishing) {
                        markSeen()
                        finish()
                    }
                }
                BackHandler {
                    // 应用整合包期间不允许离开，避免安装中断后状态不一致。
                    if (state.applying) return@BackHandler
                    if (page == PAGE_CHOICE) page = PAGE_INTRO else finish()
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                    ) {
                        when (page) {
                            PAGE_CHOICE -> OnboardingPresetScreen(
                                state = state,
                                onBack = { if (!state.applying) page = PAGE_INTRO },
                                onSelect = viewModel::select,
                                onApply = viewModel::applySelected,
                                onRetry = viewModel::retry,
                                onSkip = viewModel::completeWithoutPreset,
                            )
                            else -> OnboardingIntroScreen(
                                onContinue = { page = PAGE_CHOICE },
                                onStartOver = viewModel::completeWithoutPreset,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun markSeen() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SEEN, true)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "preset_onboarding"
        const val KEY_SEEN = "preset_prompt_seen"
        const val PAGE_INTRO = 0
        const val PAGE_CHOICE = 1
    }
}
