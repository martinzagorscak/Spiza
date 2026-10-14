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
    override val selectedHouseholdId: Flow<String?> = dataSource.selectedHouseholdId
    override val suggestionsCachingEnabled: Flow<Boolean> = dataSource.suggestionsCachingEnabled

    override suspend fun setThemeMode(mode: ThemeMode) = dataSource.setThemeMode(mode)

    override suspend fun setLanguage(language: AppLanguage) = dataSource.setLanguage(language)

    override suspend fun setSelectedHouseholdId(householdId: String) = dataSource.setSelectedHouseholdId(householdId)

    override suspend fun setSuggestionsCachingEnabled(enabled: Boolean) = dataSource.setSuggestionsCachingEnabled(enabled)
}
