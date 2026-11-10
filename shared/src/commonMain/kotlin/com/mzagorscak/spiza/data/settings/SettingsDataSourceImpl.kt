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
    private val languageState = MutableStateFlow(readLanguage())
    private val selectedHouseholdIdState = MutableStateFlow(readSelectedHouseholdId())
    private val suggestionsSavingEnabledState = MutableStateFlow(readSuggestionsSavingEnabled())
    private val dismissedRecommendedVersionState = MutableStateFlow(readDismissedRecommendedVersion())
    private val onboardingCompletedState = MutableStateFlow(readOnboardingCompleted())
    private val shoppingListLockedState = MutableStateFlow(readShoppingListLocked())

    override val themeMode: StateFlow<ThemeMode> = themeModeState.asStateFlow()
    override val language: StateFlow<AppLanguage> = languageState.asStateFlow()
    override val currentThemeMode: ThemeMode get() = themeModeState.value
    override val currentLanguage: AppLanguage get() = languageState.value
    override val selectedHouseholdId: StateFlow<String?> = selectedHouseholdIdState.asStateFlow()
    override val suggestionsSavingEnabled: StateFlow<Boolean> = suggestionsSavingEnabledState.asStateFlow()
    override val dismissedRecommendedVersion: StateFlow<Long?> = dismissedRecommendedVersionState.asStateFlow()
    override val onboardingCompleted: StateFlow<Boolean> = onboardingCompletedState.asStateFlow()
    override val shoppingListLocked: StateFlow<Boolean> = shoppingListLockedState.asStateFlow()

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

    override fun setShoppingListLocked(locked: Boolean) {
        settings.putBoolean(KEY_SHOPPING_LIST_LOCKED, locked)
        shoppingListLockedState.value = locked
    }

    // Only our own keys: on iOS the no-arg Settings is the shared NSUserDefaults, which also holds SDK (Firebase, Google) entries.
    override fun clear() {
        CLEARABLE_KEYS.forEach { settings.remove(it) }
        themeModeState.value = readThemeMode()
        languageState.value = readLanguage()
        selectedHouseholdIdState.value = readSelectedHouseholdId()
        suggestionsSavingEnabledState.value = readSuggestionsSavingEnabled()
        onboardingCompletedState.value = readOnboardingCompleted()
        shoppingListLockedState.value = readShoppingListLocked()
    }

    private fun readThemeMode(): ThemeMode =
        settings.getStringOrNull(KEY_THEME_MODE)?.let { stored ->
            ThemeMode.entries.firstOrNull { it.name == stored }
        } ?: ThemeMode.System

    private fun readLanguage(): AppLanguage = AppLanguage.fromTag(settings.getStringOrNull(KEY_LANGUAGE))

    private fun readSelectedHouseholdId(): String? = settings.getStringOrNull(KEY_SELECTED_HOUSEHOLD_ID)

    private fun readSuggestionsSavingEnabled(): Boolean = settings.getBoolean(KEY_SUGGESTIONS_SAVING_ENABLED, false)

    private fun readDismissedRecommendedVersion(): Long? = settings.getLongOrNull(KEY_DISMISSED_RECOMMENDED_VERSION)

    private fun readOnboardingCompleted(): Boolean = settings.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    private fun readShoppingListLocked(): Boolean = settings.getBoolean(KEY_SHOPPING_LIST_LOCKED, false)

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_LANGUAGE = "language"
        const val KEY_SELECTED_HOUSEHOLD_ID = "selected_household_id"
        const val KEY_SUGGESTIONS_SAVING_ENABLED = "suggestions_saving_enabled"
        const val KEY_DISMISSED_RECOMMENDED_VERSION = "dismissed_recommended_version"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val KEY_SHOPPING_LIST_LOCKED = "shopping_list_locked"

        // What clear() removes: a new preference must be added here too. The dismissed recommended update is not listed because
        // it describes the installed app on this device, not the user's data.
        val CLEARABLE_KEYS =
            listOf(
                KEY_THEME_MODE,
                KEY_LANGUAGE,
                KEY_SELECTED_HOUSEHOLD_ID,
                KEY_SUGGESTIONS_SAVING_ENABLED,
                KEY_ONBOARDING_COMPLETED,
                KEY_SHOPPING_LIST_LOCKED,
            )
    }
}
