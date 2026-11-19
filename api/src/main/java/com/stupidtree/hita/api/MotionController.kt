package com.stupidtree.hita.api

import androidx.lifecycle.LiveData

/** 宿主当前使用的动效节奏；不是插件内部的强制动画实现。 */
enum class MotionEasing {
    STANDARD,
    EMPHASIZED,
    LINEAR,
}

data class MotionTokens(
    val enabled: Boolean = true,
    val durationShortMs: Int = 300,
    val durationMediumMs: Int = 400,
    val durationLongMs: Int = 500,
    val easing: MotionEasing = MotionEasing.STANDARD,
    /** 0..1；系统动画时长缩放。0 表示系统要求移除动画。 */
    val scale: Float = 1f,
)

/**
 * 宿主提供的动效节奏。
 *
 * 插件应读取 [tokens] 来构建自己的 Compose 动画；不要假设固定时长。
 * 宿主 Shell 的页面切换也使用同一组 token，因此切换到 reduced 或系统关闭动画时，
 * 宿主与插件会同步收敛。
 */
interface MotionController {
    fun tokens(): MotionTokens
    fun observe(): LiveData<MotionTokens>

    /** standard / expressive / reduced；非法值返回 false。 */
    fun setProfile(profile: String): Boolean
    fun profile(): String

    companion object {
        val KEY = ServiceKey<MotionController>("hita.motion")
    }
}
