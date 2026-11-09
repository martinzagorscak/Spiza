package com.mzagorscak.spiza.data.settings

import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists user preferences with multiplatform-settings and exposes them as hot flows. */
internal class SettingsDataSourceImpl(
    private val settings: Settings,
) : SettingsDataSource {
    private val themeModeState = MutableStateFlow(readThemeMode())
    private val languageState = MutableStateFlow(AppLanguage.fromTag(settings.getStringOrNull(KEY_LANGUAGE)))
    private val selectedHouseholdIdState = MutableStateFlow(settings.getStringOrNull(KEY_SELECTED_HOUSEHOLD_ID))
    private val suggestionsSavingEnabledState = MutableStateFlow(settings.getBoolean(KEY_SUGGESTIONS_SAVING_ENABLED, false))
    private val dismissedRecommendedVersionState = MutableStateFlow(settings.getLongOrNull(KEY_DISMISSED_RECOMMENDED_VERSION))
    private val onboardingCompletedState = MutableStateFlow(settings.getBoolean(KEY_ONBOARDING_COMPLETED, false))

    override val themeMode: StateFlow<ThemeMode> = themeModeState.asStateFlow()
    override val language: StateFlow<AppLanguage> = languageState.asStateFlow()
    override val currentThemeMode: ThemeMode get() = themeModeState.value
    override val currentLanguage: AppLanguage get() = languageState.value
    override val selectedHouseholdId: StateFlow<String?> = selectedHouseholdIdState.asStateFlow()
    override val suggestionsSavingEnabled: StateFlow<Boolean> = suggestionsSavingEnabledState.asStateFlow()
    override val dismissedRecommendedVersion: StateFlow<Long?> = dismissedRecommendedVersionState.asStateFlow()
    override val onboardingCompleted: StateFlow<Boolean> = onboardingCompletedState.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        settings.putString(KEY_THEME_MODE, mode.name)
        themeModeState.value = mode
    }

    override fun setLanguage(language: AppLanguage) {
        settings.putString(KEY_LANGUAGE, language.tag)
        languageState.value = language
    }

    override fun setSelectedHouseholdId(householdId: String) {
        settings.putString(KEY_SELECTED_HOUSEHOLD_ID, householdId)
        selectedHouseholdIdState.value = householdId
    }

    override fun setSuggestionsSavingEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_SUGGESTIONS_SAVING_ENABLED, enabled)
        suggestionsSavingEnabledState.value = enabled
    }

    override fun setDismissedRecommendedVersion(version: Long) {
        settings.putLong(KEY_DISMISSED_RECOMMENDED_VERSION, version)
        dismissedRecommendedVersionState.value = version
    }

    override fun setOnboardingCompleted() {
        settings.putBoolean(KEY_ONBOARDING_COMPLETED, true)
        onboardingCompletedState.value = true
    }

    private fun readThemeMode(): ThemeMode =
        settings.getStringOrNull(KEY_THEME_MODE)?.let { stored ->
            ThemeMode.entries.firstOrNull { it.name == stored }
        } ?: ThemeMode.System

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_LANGUAGE = "language"
        const val KEY_SELECTED_HOUSEHOLD_ID = "selected_household_id"
        const val KEY_SUGGESTIONS_SAVING_ENABLED = "suggestions_saving_enabled"
        const val KEY_DISMISSED_RECOMMENDED_VERSION = "dismissed_recommended_version"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
}
