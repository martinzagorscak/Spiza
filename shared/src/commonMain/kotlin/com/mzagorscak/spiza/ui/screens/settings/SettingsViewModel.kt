package com.mzagorscak.spiza.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.mzagorscak.spiza.ui.model.PresentableUserProfile
import kotlinx.coroutines.flow.StateFlow

data class SettingsState(
    val profile: PresentableUserProfile = PlaceholderProfile,
    val showHouseholdJoinKeySection: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val language: AppLanguage = AppLanguage.Default,
    val isSigningOut: Boolean = false,
    val hasError: Boolean = false,
) {
    companion object {
        val PlaceholderProfile = PresentableUserProfile(
            name = "",
            email = "",
            photoUrl = null,
            joinKey = "",
        )
    }
}

abstract class SettingsViewModel : ViewModel() {
    abstract fun state(): StateFlow<SettingsState>

    abstract fun createJoinKey()

    abstract fun refreshJoinKey()

    abstract fun onThemeModeSelected(mode: ThemeMode)

    abstract fun onLanguageSelected(language: AppLanguage)

    abstract fun onSignOut()

    abstract fun onErrorShown()
}
