package com.mzagorscak.spiza.ui.screens.app

import androidx.lifecycle.ViewModel
import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

data class AppState(
    val themeMode: ThemeMode,
    val language: AppLanguage,
    val isSignedIn: Boolean,
    val isOnboardingCompleted: Boolean,
)

/** Exposes app-wide state applied at the root of the UI: preferences (theme, language) and what decides the start screen. */
abstract class AppViewModel : ViewModel() {
    abstract fun state(): StateFlow<AppState>
}
