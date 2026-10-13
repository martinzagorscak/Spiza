package com.mzagorscak.spiza.ui.screens.settings

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.mzagorscak.spiza.domain.usecase.ObserveAppLanguageUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveThemeModeUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveUserProfileUseCase
import com.mzagorscak.spiza.domain.usecase.SetAppLanguageUseCase
import com.mzagorscak.spiza.domain.usecase.SetThemeModeUseCase
import com.mzagorscak.spiza.domain.usecase.SignOutUseCase
import com.mzagorscak.spiza.ui.model.toPresentable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class SettingsViewModelImpl(
    observeUserProfile: ObserveUserProfileUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
    observeAppLanguage: ObserveAppLanguageUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setAppLanguage: SetAppLanguageUseCase,
    private val signOut: SignOutUseCase,
) : SettingsViewModel() {
    private val state = MutableStateFlow(SettingsState())

    init {
        observeUserProfile()
            .onEach { profile -> state.update { it.copy(profile = profile?.toPresentable()) } }
            .launchIn(viewModelScope)
        observeThemeMode()
            .onEach { mode -> state.update { it.copy(themeMode = mode) } }
            .launchIn(viewModelScope)
        observeAppLanguage()
            .onEach { language -> state.update { it.copy(language = language) } }
            .launchIn(viewModelScope)
    }

    override fun state(): StateFlow<SettingsState> = state.asStateFlow()

    override fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }

    override fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch { setAppLanguage(language) }
    }

    // Navigation reacts to the auth state change, so there is nothing to do on success.
    override fun onSignOutClicked() {
        if (state.value.isSigningOut) return
        viewModelScope.launch {
            state.update { it.copy(isSigningOut = true) }
            try {
                signOut()
            } finally {
                state.update { it.copy(isSigningOut = false) }
            }
        }
    }
}
