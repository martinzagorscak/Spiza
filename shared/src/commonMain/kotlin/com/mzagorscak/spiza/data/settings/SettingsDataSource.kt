package com.mzagorscak.spiza.data.settings

import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

interface SettingsDataSource {
    val themeMode: StateFlow<ThemeMode>
    val language: StateFlow<AppLanguage>
    val currentThemeMode: ThemeMode
    val currentLanguage: AppLanguage
    val selectedHouseholdId: StateFlow<String?>
    val suggestionsSavingEnabled: StateFlow<Boolean>
    val dismissedRecommendedVersion: StateFlow<Long?>
    val onboardingCompleted: StateFlow<Boolean>
    val shoppingListLocked: StateFlow<Boolean>

    fun setThemeMode(mode: ThemeMode)

    fun setLanguage(language: AppLanguage)

    fun setSelectedHouseholdId(householdId: String)

    fun setSuggestionsSavingEnabled(enabled: Boolean)

    fun setDismissedRecommendedVersion(version: Long)

    fun setOnboardingCompleted()

    fun setShoppingListLocked(locked: Boolean)

    /**
     * Removes the user's preferences (only the app's own keys; the dismissed recommended update stays) and resets their flows
     * to the defaults.
     */
    fun clear()
}
