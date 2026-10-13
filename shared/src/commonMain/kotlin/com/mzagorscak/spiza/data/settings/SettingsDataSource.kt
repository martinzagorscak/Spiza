package com.mzagorscak.spiza.data.settings

import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists user preferences with multiplatform-settings and exposes them as hot flows. */
class SettingsDataSource(
    private val settings: Settings,
) {
    private val themeModeState = MutableStateFlow(readThemeMode())
    private val languageState = MutableStateFlow(AppLanguage.fromTag(settings.getStringOrNull(KEY_LANGUAGE)))

    val themeMode: StateFlow<ThemeMode> = themeModeState.asStateFlow()
    val language: StateFlow<AppLanguage> = languageState.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        settings.putString(KEY_THEME_MODE, mode.name)
        themeModeState.value = mode
    }

    fun setLanguage(language: AppLanguage) {
        settings.putString(KEY_LANGUAGE, language.tag)
        languageState.value = language
    }

    private fun readThemeMode(): ThemeMode =
        settings.getStringOrNull(KEY_THEME_MODE)?.let { stored ->
            ThemeMode.entries.firstOrNull { it.name == stored }
        } ?: ThemeMode.System

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_LANGUAGE = "language"
    }
}
