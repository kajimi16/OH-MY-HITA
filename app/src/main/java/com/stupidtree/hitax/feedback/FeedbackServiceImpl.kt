package com.stupidtree.hitax.feedback

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.stupidtree.hita.api.FeedbackEvent
import com.stupidtree.hita.api.FeedbackService
import com.stupidtree.hita.api.FeedbackSettings
import com.stupidtree.hita.api.HapticType

/**
 * 宿主统一反馈实现。
 *
 * 前置条件：调用方只传 API 枚举，不传音量/设备句柄。后置条件：静音时 [play] 不发声，
 * 关闭触感时 [haptic] 不振动。不变式：调用方不能绕过 [FeedbackSettings]。
 * 违约处理：设备无振动器或音频初始化失败时静默忽略，不影响业务。
 */
class FeedbackServiceImpl(context: Context) : FeedbackService {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("feedback", Context.MODE_PRIVATE)
    private val live = MutableLiveData(settingsInternal())
    private var tone: ToneGenerator? = null

    override fun play(event: FeedbackEvent) {
        val settings = settingsInternal()
        if (!settings.soundEnabled || settings.volume <= 0f) return
        runCatching {
            val generator = tone
                ?: ToneGenerator(AudioManager.STREAM_MUSIC, (settings.volume * 100).toInt()).also {
                    tone = it
                }
            val (toneType, durationMs) = when (event) {
                FeedbackEvent.TAB -> ToneGenerator.TONE_PROP_BEEP to 55
                FeedbackEvent.CLICK -> ToneGenerator.TONE_PROP_BEEP to 45
                FeedbackEvent.SUCCESS -> ToneGenerator.TONE_PROP_ACK to 90
                FeedbackEvent.ERROR -> ToneGenerator.TONE_SUP_ERROR to 110
                FeedbackEvent.NOTICE -> ToneGenerator.TONE_PROP_PROMPT to 80
            }
            generator.startTone(toneType, durationMs)
        }
    }

    override fun haptic(type: HapticType) {
        if (!settingsInternal().hapticEnabled) return
        val vibrator = vibrator() ?: return
        if (!vibrator.hasVibrator()) return
        val (durationMs, amplitude) = when (type) {
            HapticType.LIGHT -> 20L to 70
            HapticType.MEDIUM -> 35L to 120
            HapticType.HEAVY -> 55L to 180
            HapticType.SUCCESS -> 40L to 110
            HapticType.WARNING -> 60L to 160
            HapticType.ERROR -> 80L to 220
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    }

    override fun settings(): FeedbackSettings = settingsInternal()

    override fun observeSettings(): LiveData<FeedbackSettings> = live

    override fun updateSettings(settings: FeedbackSettings) {
        val normalized = settings.copy(volume = settings.volume.coerceIn(0f, 1f))
        prefs.edit()
            .putBoolean(KEY_SOUND, normalized.soundEnabled)
            .putBoolean(KEY_HAPTIC, normalized.hapticEnabled)
            .putFloat(KEY_VOLUME, normalized.volume)
            .apply()
        tone?.release()
        tone = null
        live.postValue(normalized)
    }

    private fun settingsInternal(): FeedbackSettings = FeedbackSettings(
        soundEnabled = prefs.getBoolean(KEY_SOUND, false),
        hapticEnabled = prefs.getBoolean(KEY_HAPTIC, false),
        volume = prefs.getFloat(KEY_VOLUME, 1f).coerceIn(0f, 1f),
    )

    private fun vibrator(): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            app.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    private companion object {
        const val KEY_SOUND = "sound_enabled"
        const val KEY_HAPTIC = "haptic_enabled"
        const val KEY_VOLUME = "volume"
    }
}
