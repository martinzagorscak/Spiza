package com.mzagorscak.spiza.ui.screens.settings

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.crash.catchAndRecord
import com.mzagorscak.spiza.crash.dataErrorReason
import com.mzagorscak.spiza.crash.isQuotaExceeded
import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.DeleteAccountResult
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.mzagorscak.spiza.domain.usecase.CanUserJoinHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.CreateHouseholdJoinKeyUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteAccountUseCase
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
    private val deleteAccount: DeleteAccountUseCase,
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
        if (state.value.isSigningOut || state.value.isDeletingAccount) return
        state.update { it.copy(isSigningOut = true) }
        launchAction(context = "Settings sign-out failed") {
            try {
                signOut()
            } finally {
                state.update { it.copy(isSigningOut = false) }
            }
        }
    }

    // Like sign-out, navigation reacts to the auth state change on success; a cancelled confirmation shows nothing.
    // The repository finishes a confirmed deletion even when this scope is cancelled by leaving the screen.
    override fun onDeleteAccount() {
        if (state.value.isDeletingAccount || state.value.isSigningOut) return
        state.update { it.copy(isDeletingAccount = true) }
        launchSafely(
            crashLogger = crashLogger,
            context = "Settings account deletion failed",
            onError = { error -> state.update { it.copy(errorState = error.toDeleteAccountErrorState()) } },
        ) {
            try {
                if (deleteAccount() == DeleteAccountResult.WrongAccount) {
                    state.update { it.copy(errorState = SettingsState.ErrorState.DeleteAccountWrongAccount) }
                }
            } finally {
                state.update { it.copy(isDeletingAccount = false) }
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

private fun Throwable.toDeleteAccountErrorState(): SettingsState.ErrorState =
    when {
        dataErrorReason() == DataException.Reason.Offline -> SettingsState.ErrorState.Offline
        isQuotaExceeded() -> SettingsState.ErrorState.QuotaExceeded
        else -> SettingsState.ErrorState.Generic
    }
