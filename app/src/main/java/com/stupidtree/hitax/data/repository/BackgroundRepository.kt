package com.stupidtree.hitax.data.repository

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MediatorLiveData
import com.stupidtree.component.data.booleanLiveData
import com.stupidtree.component.data.intLiveData
import com.stupidtree.component.data.stringLiveData

data class BackgroundSettings(
    var imageUri: String = "",
    var transparency: Int = 30,
    var blurRadius: Int = 8,
    var scopeGlobal: Boolean = false,
    var enabled: Boolean = false,
    var bgColor: String = "",
    var fitMode: String = "crop"
)

class BackgroundRepository(application: Application) {
    private val sp: SharedPreferences =
        application.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    val imageUriLiveData = sp.stringLiveData(KEY_IMAGE_URI, "")
    val transparencyLiveData = sp.intLiveData(KEY_TRANSPARENCY, 30)
    val blurRadiusLiveData = sp.intLiveData(KEY_BLUR_RADIUS, 8)
    val scopeGlobalLiveData = sp.booleanLiveData(KEY_SCOPE_GLOBAL, false)
    val enabledLiveData = sp.booleanLiveData(KEY_ENABLED, false)

    fun getImageUri(): String = sp.getString(KEY_IMAGE_URI, "") ?: ""
    fun setImageUri(uri: String) { sp.edit().putString(KEY_IMAGE_URI, uri).apply() }

    fun getTransparency(): Int = sp.getInt(KEY_TRANSPARENCY, 30)
    fun setTransparency(v: Int) { sp.edit().putInt(KEY_TRANSPARENCY, v).apply() }

    fun getBlurRadius(): Int = sp.getInt(KEY_BLUR_RADIUS, 8)
    fun setBlurRadius(v: Int) { sp.edit().putInt(KEY_BLUR_RADIUS, v).apply() }

    fun isScopeGlobal(): Boolean = sp.getBoolean(KEY_SCOPE_GLOBAL, false)
    fun setScopeGlobal(v: Boolean) { sp.edit().putBoolean(KEY_SCOPE_GLOBAL, v).apply() }

    fun isEnabled(): Boolean = sp.getBoolean(KEY_ENABLED, false)
    fun setEnabled(v: Boolean) { sp.edit().putBoolean(KEY_ENABLED, v).apply() }

    fun getBgColor(): String = sp.getString(KEY_BG_COLOR, "") ?: ""
    fun setBgColor(hex: String) { sp.edit().putString(KEY_BG_COLOR, hex).apply() }

    fun getFitMode(): String = sp.getString(KEY_FIT_MODE, "crop") ?: "crop"
    fun setFitMode(mode: String) { sp.edit().putString(KEY_FIT_MODE, mode).apply() }

    val bgColorLiveData = sp.stringLiveData(KEY_BG_COLOR, "")
    val fitModeLiveData = sp.stringLiveData(KEY_FIT_MODE, "crop")

    fun getSettingsSnapshot(): BackgroundSettings = BackgroundSettings(
        imageUri = getImageUri(),
        transparency = getTransparency(),
        blurRadius = getBlurRadius(),
        scopeGlobal = isScopeGlobal(),
        enabled = isEnabled(),
        bgColor = getBgColor(),
        fitMode = getFitMode()
    )

    fun getSettingsLiveData(): MediatorLiveData<BackgroundSettings> {
        val ml = MediatorLiveData<BackgroundSettings>()
        ml.value = getSettingsSnapshot()
        ml.addSource(imageUriLiveData) { ml.value = ml.value?.copy(imageUri = it ?: "") }
        ml.addSource(transparencyLiveData) { ml.value = ml.value?.copy(transparency = it ?: 30) }
        ml.addSource(blurRadiusLiveData) { ml.value = ml.value?.copy(blurRadius = it ?: 8) }
        ml.addSource(scopeGlobalLiveData) { ml.value = ml.value?.copy(scopeGlobal = it ?: false) }
        ml.addSource(enabledLiveData) { ml.value = ml.value?.copy(enabled = it ?: false) }
        ml.addSource(bgColorLiveData) { ml.value = ml.value?.copy(bgColor = it ?: "") }
        ml.addSource(fitModeLiveData) { ml.value = ml.value?.copy(fitMode = it ?: "crop") }
        return ml
    }

    companion object {
        const val SP_NAME = "background_settings"
        const val KEY_IMAGE_URI = "image_uri"
        const val KEY_TRANSPARENCY = "transparency"
        const val KEY_BLUR_RADIUS = "blur_radius"
        const val KEY_SCOPE_GLOBAL = "scope_global"
        const val KEY_ENABLED = "enabled"
        const val KEY_BG_COLOR = "bg_color"
        const val KEY_FIT_MODE = "fit_mode"

        private var instance: BackgroundRepository? = null
        fun getInstance(application: Application): BackgroundRepository {
            if (instance == null) instance = BackgroundRepository(application)
            return instance!!
        }
    }
}
