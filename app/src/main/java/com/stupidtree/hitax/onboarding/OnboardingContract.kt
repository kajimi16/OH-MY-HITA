package com.stupidtree.hitax.onboarding

/**
 * Frozen contract for the first-run onboarding redesign.
 * UI workers may not change these types; coordinator owns this file.
 */
data class OnboardingPresetUi(
    val id: String,
    val name: String,
    val description: String,
    val source: String?,
    val pluginCount: Int,
    val recommended: Boolean,
    val active: Boolean,
)

data class OnboardingUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val presets: List<OnboardingPresetUi> = emptyList(),
    val selectedId: String? = null,
    val applying: Boolean = false,
    val applyMessage: String? = null,
    val completed: Boolean = false,
)

interface OnboardingActions {
    fun retry()
    fun select(id: String)
    fun applySelected()
    fun startFromScratch()
    fun skip()
}
