package com.mzagorscak.spiza.ui.screens.settings

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.catchAndRecord
import com.mzagorscak.spiza.crash.isQuotaExceeded
import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.mzagorscak.spiza.domain.usecase.CanUserJoinHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.CreateHouseholdJoinKeyUseCase
import com.mzagorscak.spiza.domain.usecase.GetHouseholdJoinKeyUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveAppLanguageUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveThemeModeUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveUserProfileUseCase
import com.mzagorscak.spiza.domain.usecase.RefreshHouseholdJoinKeyUseCase
import com.mzagorscak.spiza.domain.usecase.SetAppLanguageUseCase
import com.mzagorscak.spiza.domain.usecase.SetThemeModeUseCase
import com.mzagorscak.spiza.domain.usecase.SignOutUseCase
import com.mzagorscak.spiza.ui.model.toPresentable
import com.mzagorscak.spiza.ui.screens.common.launchSafely
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
internal class SettingsViewModelImpl(
    observeUserProfile: ObserveUserProfileUseCase,
    private val canUserJoinHousehold: CanUserJoinHouseholdUseCase,
    private val createHouseholdJoinKey: CreateHouseholdJoinKeyUseCase,
    getHouseholdJoinKey: GetHouseholdJoinKeyUseCase,
    private val refreshHouseholdJoinKey: RefreshHouseholdJoinKeyUseCase,
    observeThemeMode: ObserveThemeModeUseCase,
    observeAppLanguage: ObserveAppLanguageUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setAppLanguage: SetAppLanguageUseCase,
    private val signOut: SignOutUseCase,
    private val crashLogger: CrashLogger,
) : SettingsViewModel() {
    private val state = MutableStateFlow(SettingsState())

    init {
        combine(
            observeUserProfile(),
            canUserJoinHousehold(),
            getHouseholdJoinKey(),
            observeThemeMode(),
            observeAppLanguage(),
        ) { userProfile, canUserJoinHousehold, joinKey, themeMode, appLanguage ->
            state.update {
                it.copy(
                    profile = userProfile?.toPresentable(joinKey) ?: SettingsState.PlaceholderProfile,
                    showHouseholdJoinKeySection = canUserJoinHousehold,
                    themeMode = themeMode,
                    language = appLanguage,
                )
            }
        }.catchAndRecord(crashLogger = crashLogger, context = "Settings state stream failed")
            .launchIn(viewModelScope)
    }

    override fun state(): StateFlow<SettingsState> = state.asStateFlow()

    override fun createJoinKey() {
        launchAction(context = "Settings join key creation failed") { createHouseholdJoinKey() }
    }

    override fun refreshJoinKey() {
        launchAction(context = "Settings join key refresh failed") { refreshHouseholdJoinKey() }
    }

    override fun onThemeModeSelected(mode: ThemeMode) {
        launchAction(context = "Settings theme change failed") { setThemeMode(mode) }
    }

    override fun onLanguageSelected(language: AppLanguage) {
        launchAction(context = "Settings language change failed") { setAppLanguage(language) }
    }

    // Navigation reacts to the auth state change, so there is nothing to do on success.
    override fun onSignOut() {
        if (state.value.isSigningOut) return
        state.update { it.copy(isSigningOut = true) }
        launchAction(context = "Settings sign-out failed") {
            try {
                signOut()
            } finally {
                state.update { it.copy(isSigningOut = false) }
            }
        }
    }

    override fun onErrorShown() {
        state.update { it.copy(errorState = SettingsState.ErrorState.None) }
    }

    private fun launchAction(
        context: String,
        block: suspend () -> Unit,
    ) {
        launchSafely(
            crashLogger = crashLogger,
            context = context,
            onError = { error -> state.update { it.copy(errorState = error.toErrorState()) } },
        ) { block() }
    }
}

private fun Throwable.toErrorState(): SettingsState.ErrorState =
    if (isQuotaExceeded()) {
        SettingsState.ErrorState.QuotaExceeded
    } else {
        SettingsState.ErrorState.Generic
    }
