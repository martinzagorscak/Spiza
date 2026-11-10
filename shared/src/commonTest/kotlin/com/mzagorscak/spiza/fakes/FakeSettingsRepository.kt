package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.mzagorscak.spiza.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** In-memory settings; every setter is reflected in its stream and `current…` value like the persisted settings. */
class FakeSettingsRepository : SettingsRepository {
    val selectedId = MutableStateFlow<String?>(null)
    val savingEnabled = MutableStateFlow(false)
    val listLocked = MutableStateFlow(false)

    val themeModes = MutableStateFlow(ThemeMode.System)
    val languages = MutableStateFlow(AppLanguage.Default)

    /** When set, [themeMode] fails with it (checked each time the stream is collected). */
    var themeModeFailure: Throwable? = null

    /** When set, [setShoppingListLocked] throws it after recording the attempt. */
    var setShoppingListLockedFailure: Throwable? = null

    val shoppingListLockedWrites = mutableListOf<Boolean>()

    override val themeMode: Flow<ThemeMode> =
        flow {
            themeModeFailure?.let { throw it }
            emitAll(themeModes)
        }
    override val language: Flow<AppLanguage> = languages
    override val currentThemeMode: ThemeMode get() = themeModes.value
    override val currentLanguage: AppLanguage get() = languages.value
    override val selectedHouseholdId: Flow<String?> = selectedId
    override val suggestionsSavingEnabled: Flow<Boolean> = savingEnabled
    override val shoppingListLocked: Flow<Boolean> = listLocked

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeModes.value = mode
    }

    override suspend fun setLanguage(language: AppLanguage) {
        languages.value = language
    }

    override suspend fun setSelectedHouseholdId(householdId: String) {
        selectedId.value = householdId
    }

    override suspend fun setSuggestionsSavingEnabled(enabled: Boolean) {
        savingEnabled.value = enabled
    }

    override suspend fun setShoppingListLocked(locked: Boolean) {
        shoppingListLockedWrites += locked
        setShoppingListLockedFailure?.let { throw it }
        listLocked.value = locked
    }
}
