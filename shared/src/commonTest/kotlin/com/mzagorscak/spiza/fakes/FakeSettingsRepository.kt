package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.mzagorscak.spiza.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository : SettingsRepository {
    val selectedId = MutableStateFlow<String?>(null)
    val savingEnabled = MutableStateFlow(false)

    override val themeMode: Flow<ThemeMode> = MutableStateFlow(ThemeMode.System)
    override val language: Flow<AppLanguage> = MutableStateFlow(AppLanguage.Default)
    override val currentThemeMode: ThemeMode = ThemeMode.System
    override val currentLanguage: AppLanguage = AppLanguage.Default
    override val selectedHouseholdId: Flow<String?> = selectedId
    override val suggestionsSavingEnabled: Flow<Boolean> = savingEnabled

    override suspend fun setThemeMode(mode: ThemeMode) = Unit

    override suspend fun setLanguage(language: AppLanguage) = Unit

    override suspend fun setSelectedHouseholdId(householdId: String) {
        selectedId.value = householdId
    }

    override suspend fun setSuggestionsSavingEnabled(enabled: Boolean) {
        savingEnabled.value = enabled
    }
}
