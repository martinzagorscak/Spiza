package com.mzagorscak.spiza.data.settings

import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

interface SettingsDataSource {
    val themeMode: StateFlow<ThemeMode>
    val language: StateFlow<AppLanguage>
    val selectedHouseholdId: StateFlow<String?>
    val suggestionsSavingEnabled: StateFlow<Boolean>

    fun setThemeMode(mode: ThemeMode)

    fun setLanguage(language: AppLanguage)

    fun setSelectedHouseholdId(householdId: String)

    fun setSuggestionsSavingEnabled(enabled: Boolean)
}
