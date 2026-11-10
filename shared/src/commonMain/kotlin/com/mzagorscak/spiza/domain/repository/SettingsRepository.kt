package com.mzagorscak.spiza.domain.repository

import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val language: Flow<AppLanguage>
    val currentThemeMode: ThemeMode
    val currentLanguage: AppLanguage
    val selectedHouseholdId: Flow<String?>
    val suggestionsSavingEnabled: Flow<Boolean>
    val shoppingListLocked: Flow<Boolean>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setLanguage(language: AppLanguage)

    suspend fun setSelectedHouseholdId(householdId: String)

    suspend fun setSuggestionsSavingEnabled(enabled: Boolean)

    suspend fun setShoppingListLocked(locked: Boolean)
}
