package com.stupidtree.hitax.motion

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stupidtree.hita.api.MotionController
import com.stupidtree.hita.api.MotionEasing
import com.stupidtree.hita.api.MotionTokens
import kotlin.math.roundToInt

/**
 * 宿主动效节奏实现。
 *
 * 前置条件：应用 Context 可用。后置条件：[tokens] 始终返回已按系统动画缩放收敛的值。
 * 不变式：系统 `ANIMATOR_DURATION_SCALE == 0` 时 [MotionTokens.enabled] 必为 false。
 * 违约处理：系统设置读取失败时按 1.0 处理，不阻断界面。
 */
class MotionControllerImpl(context: Context) : MotionController {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("motion", Context.MODE_PRIVATE)
    private val live = MutableLiveData(tokensInternal())

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            live.postValue(tokensInternal())
        }
    }

    init {
        runCatching {
            app.contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                false,
                observer,
            )
        }
    }

    override fun tokens(): MotionTokens = tokensInternal()

    override fun observe(): LiveData<MotionTokens> = live

    override fun setProfile(profile: String): Boolean {
        if (profile !in PROFILES) return false
        prefs.edit().putString(KEY_PROFILE, profile).apply()
        live.postValue(tokensInternal())
        return true
    }

    override fun profile(): String = prefs.getString(KEY_PROFILE, PROFILE_STANDARD)
        ?.takeIf { it in PROFILES } ?: PROFILE_STANDARD

    private fun tokensInternal(): MotionTokens {
        val systemScale = runCatching {
            Settings.Global.getFloat(app.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f).coerceIn(0f, 1f)
        val profile = profile()
        val durations = when (profile) {
            PROFILE_EXPRESSIVE -> intArrayOf(400, 500, 600)
            PROFILE_REDUCED -> intArrayOf(0, 0, 0)
            else -> intArrayOf(300, 400, 500)
        }
        val enabled = profile != PROFILE_REDUCED && systemScale > 0.01f
        val scale = if (enabled) systemScale else 0f
        return MotionTokens(
            enabled = enabled,
            durationShortMs = (durations[0] * scale).roundToInt(),
            durationMediumMs = (durations[1] * scale).roundToInt(),
            durationLongMs = (durations[2] * scale).roundToInt(),
            easing = when (profile) {
                PROFILE_EXPRESSIVE -> MotionEasing.EMPHASIZED
                PROFILE_REDUCED -> MotionEasing.LINEAR
                else -> MotionEasing.STANDARD
            },
            scale = scale,
        )
    }

    private companion object {
        const val KEY_PROFILE = "profile"
        const val PROFILE_STANDARD = "standard"
        const val PROFILE_EXPRESSIVE = "expressive"
        const val PROFILE_REDUCED = "reduced"
        val PROFILES = setOf(PROFILE_STANDARD, PROFILE_EXPRESSIVE, PROFILE_REDUCED)
    }
}
