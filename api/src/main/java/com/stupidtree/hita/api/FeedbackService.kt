package com.stupidtree.hita.api

import androidx.lifecycle.LiveData

enum class FeedbackEvent {
    TAB,
    CLICK,
    SUCCESS,
    ERROR,
    NOTICE,
}

enum class HapticType {
    LIGHT,
    MEDIUM,
    HEAVY,
    SUCCESS,
    WARNING,
    ERROR,
}

data class FeedbackSettings(
    val soundEnabled: Boolean = false,
    val hapticEnabled: Boolean = false,
    /** 0..1；宿主会再受系统音量与静音模式约束。 */
    val volume: Float = 1f,
)

/**
 * 语义化的 UI 音效/触感。
 *
 * 插件只请求“发生了什么”，不直接持有 SoundPool/Vibrator；宿主统一处理静音、
 * 系统触感设置与通知渠道。非 UI 的通知声音不属于本服务。
 */
interface FeedbackService {
    fun play(event: FeedbackEvent)
    fun haptic(type: HapticType)
    fun settings(): FeedbackSettings
    fun observeSettings(): LiveData<FeedbackSettings>
    fun updateSettings(settings: FeedbackSettings)

    companion object {
        val KEY = ServiceKey<FeedbackService>("hita.feedback")
    }
}
