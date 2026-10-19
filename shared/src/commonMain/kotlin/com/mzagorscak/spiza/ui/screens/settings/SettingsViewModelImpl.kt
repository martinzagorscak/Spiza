package com.mzagorscak.spiza.ui.screens.settings

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.mzagorscak.spiza.domain.usecase.CreateHouseholdJoinKeyUseCase
import com.mzagorscak.spiza.domain.usecase.GetHouseholdJoinKeyUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveAppLanguageUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveThemeModeUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveUserProfileUseCase
import com.mzagorscak.spiza.domain.usecase.SetAppLanguageUseCase
import com.mzagorscak.spiza.domain.usecase.SetThemeModeUseCase
import com.mzagorscak.spiza.domain.usecase.SignOutUseCase
import com.mzagorscak.spiza.ui.model.toPresentable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
internal class SettingsViewModelImpl(
    observeUserProfile: ObserveUserProfileUseCase,
    private val createHouseholdJoinKey: CreateHouseholdJoinKeyUseCase,
    getHouseholdJoinKey: GetHouseholdJoinKeyUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
    observeAppLanguage: ObserveAppLanguageUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setAppLanguage: SetAppLanguageUseCase,
    private val signOut: SignOutUseCase,
) : SettingsViewModel() {
    private val state = MutableStateFlow(SettingsState())
    private val refreshHouseholdJoinKeyPublisher = MutableSharedFlow<Unit>()
    private val observeHouseholdJoinKey = refreshHouseholdJoinKeyPublisher
        .onStart { emit(Unit) }
        .flatMapLatest { getHouseholdJoinKey() }

    init {
        combine(
            observeUserProfile(),
            observeHouseholdJoinKey,
            observeThemeMode(),
            observeAppLanguage(),
        ) { userProfile, joinKey, themeMode, appLanguage ->
            state.update {
                it.copy(
                    profile = userProfile?.toPresentable(joinKey) ?: SettingsState.PlaceholderProfile,
                    themeMode = themeMode,
                    language = appLanguage,
                )
            }
        }.launchIn(viewModelScope)
    }

    override fun state(): StateFlow<SettingsState> = state.asStateFlow()

    override fun createJoinKey() {
        viewModelScope.launch { createHouseholdJoinKey() }
    }

    override fun refreshJoinKey() {
        viewModelScope.launch { refreshHouseholdJoinKeyPublisher.emit(Unit) }
    }

    override fun onThemeModeSelected(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }

    override fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch { setAppLanguage(language) }
    }

    // Navigation reacts to the auth state change, so there is nothing to do on success.
    override fun onSignOut() {
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
