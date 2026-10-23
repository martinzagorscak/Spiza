package com.mzagorscak.spiza.ui.screens.app

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.domain.usecase.GetCurrentAppLanguageUseCase
import com.mzagorscak.spiza.domain.usecase.GetCurrentThemeModeUseCase
import com.mzagorscak.spiza.domain.usecase.GetCurrentUserProfileUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveAppLanguageUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveThemeModeUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveUserProfileUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal class AppViewModelImpl(
    observeThemeMode: ObserveThemeModeUseCase,
    observeAppLanguage: ObserveAppLanguageUseCase,
    observeUserProfile: ObserveUserProfileUseCase,
    getCurrentThemeMode: GetCurrentThemeModeUseCase,
    getCurrentAppLanguage: GetCurrentAppLanguageUseCase,
    getCurrentUserProfile: GetCurrentUserProfileUseCase,
) : AppViewModel() {
    private val state =
        combine(
            observeThemeMode(),
            observeAppLanguage(),
            observeUserProfile(),
        ) { theme, language, profile ->
            AppState(themeMode = theme, language = language, isSignedIn = profile != null)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            AppState(
                themeMode = getCurrentThemeMode(),
                language = getCurrentAppLanguage(),
                isSignedIn = getCurrentUserProfile() != null,
            ),
        )

    override fun state(): StateFlow<AppState> = state
}
