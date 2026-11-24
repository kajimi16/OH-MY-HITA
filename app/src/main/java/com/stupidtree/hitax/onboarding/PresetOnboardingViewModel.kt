package com.stupidtree.hitax.onboarding

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.stupidtree.hitax.market.MarketPreset
import com.stupidtree.hitax.market.PluginMarketViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 首启整合包步骤的适配器：把 [PluginMarketViewModel] 的多路 LiveData
 * 收敛成一个 [OnboardingUiState] 状态流。
 *
 * 状态在主线程发布；市场实例由本 ViewModel 独占，避免重复创建后台执行器。
 */
class PresetOnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val market = PluginMarketViewModel(application)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private val cleanup = mutableListOf<() -> Unit>()

    private var presets: List<MarketPreset> = emptyList()
    private var activePresetId: String? = null
    private var loaded = false
    private var loadError: String? = null
    private var status: String? = null
    private var selectedId: String? = null
    private var applying = false
    private var applyMessage: String? = null
    private var applyError: String? = null
    private var completed = false

    init {
        market.presets.track { presets = it.orEmpty(); render() }
        market.activePresetId.track { id ->
            activePresetId = id
            if (applying && id != null && id == selectedId) markSuccess()
            render()
        }
        market.presetsLoaded.track { loaded = it; render() }
        market.loadError.track { loadError = it; render() }
        market.status.track { status = it; render() }
        market.applyProgress.track { progress ->
            if (applying) {
                applyMessage = progress?.message
                render()
            }
        }
        market.presetSwitch.track { target ->
            if (target != null) {
                if (applying && target.presetId == selectedId) markSuccess()
                market.consumePresetSwitch()
            }
        }
        market.message.track { text ->
            if (text != null) {
                if (applying) {
                    applying = false
                    applyError = text
                    applyMessage = null
                }
                market.consumeMessage()
            }
            render()
        }
        render()
    }

    fun retry() {
        applyError = null
        applyMessage = null
        loaded = false
        render()
        market.refresh()
    }

    fun select(id: String) {
        if (presets.none { it.id == id }) return
        selectedId = id
        applyError = null
        render()
    }

    fun applySelected() {
        if (applying || completed) return
        val preset = presets.firstOrNull { it.id == selectedId } ?: return
        applying = true
        applyError = null
        applyMessage = null
        render()
        market.applyPreset(preset)
    }

    fun completeWithoutPreset() {
        applying = false
        applyError = null
        applyMessage = null
        completed = true
        render()
    }

    override fun onCleared() {
        cleanup.forEach { it() }
        cleanup.clear()
        super.onCleared()
    }

    private fun markSuccess() {
        applying = false
        applyError = null
        applyMessage = null
        completed = true
    }

    private fun render() {
        val uiPresets = presets.map { preset ->
            OnboardingPresetUi(
                id = preset.id,
                name = preset.name,
                description = preset.description?.takeIf { it.isNotBlank() }
                    ?: FALLBACK_DESCRIPTION,
                source = preset.source,
                pluginCount = preset.refs().size,
                recommended = preset.id == RECOMMENDED_PRESET_ID,
                active = preset.id == activePresetId,
                family = preset.familyId(),
                version = preset.version,
            )
        }
        val resolvedSelection = selectedId
            ?.takeIf { id -> uiPresets.any { it.id == id } }
            ?: defaultSelection(uiPresets)
        selectedId = resolvedSelection
        publish(
            OnboardingUiState(
                loading = !loaded,
                error = applyError ?: loadError ?: statusLoadFailure(),
                presets = uiPresets,
                selectedId = resolvedSelection,
                applying = applying,
                applyMessage = applyMessage,
                completed = completed,
            ),
        )
    }

    private fun defaultSelection(uiPresets: List<OnboardingPresetUi>): String? =
        activePresetId?.takeIf { id -> uiPresets.any { it.id == id } }
            ?: uiPresets.firstOrNull { it.recommended }?.id
            ?: uiPresets.firstOrNull()?.id

    private fun statusLoadFailure(): String? =
        status?.takeIf { it.startsWith(LOAD_FAILURE_PREFIX) }

    private fun publish(next: OnboardingUiState) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            _state.value = next
        } else {
            mainHandler.post { _state.value = next }
        }
    }

    private fun <T> LiveData<T>.track(block: (T) -> Unit) {
        val observer = Observer<T> { block(it) }
        observeForever(observer)
        cleanup.add { removeObserver(observer) }
    }

    companion object {
        private const val RECOMMENDED_PRESET_ID = "hita-l-1.5.3"
        private const val LOAD_FAILURE_PREFIX = "读取索引失败"
        private const val FALLBACK_DESCRIPTION = "一键安装并启用该整合包中的插件"
    }
}
