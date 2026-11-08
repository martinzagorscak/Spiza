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

    fun setThemeMode(mode: ThemeMode)

    fun setLanguage(language: AppLanguage)

    fun setSelectedHouseholdId(householdId: String)

    fun setSuggestionsSavingEnabled(enabled: Boolean)

    fun setDismissedRecommendedVersion(version: Long)
}
