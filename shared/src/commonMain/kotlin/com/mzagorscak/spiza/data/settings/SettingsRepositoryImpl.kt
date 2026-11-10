package com.mzagorscak.spiza.data.settings

import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.mzagorscak.spiza.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

internal class SettingsRepositoryImpl(
    private val dataSource: SettingsDataSource,
) : SettingsRepository {
    override val themeMode: Flow<ThemeMode> = dataSource.themeMode
    override val language: Flow<AppLanguage> = dataSource.language
    override val currentThemeMode: ThemeMode get() = dataSource.currentThemeMode
    override val currentLanguage: AppLanguage get() = dataSource.currentLanguage
    override val selectedHouseholdId: Flow<String?> = dataSource.selectedHouseholdId
    override val suggestionsSavingEnabled: Flow<Boolean> = dataSource.suggestionsSavingEnabled
    override val shoppingListLocked: Flow<Boolean> = dataSource.shoppingListLocked

    override suspend fun setThemeMode(mode: ThemeMode) = dataSource.setThemeMode(mode)

    override suspend fun setLanguage(language: AppLanguage) = dataSource.setLanguage(language)

    override suspend fun setSelectedHouseholdId(householdId: String) = dataSource.setSelectedHouseholdId(householdId)

    override suspend fun setSuggestionsSavingEnabled(enabled: Boolean) = dataSource.setSuggestionsSavingEnabled(enabled)

    override suspend fun setShoppingListLocked(locked: Boolean) = dataSource.setShoppingListLocked(locked)
}
