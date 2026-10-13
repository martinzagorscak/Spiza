package com.mzagorscak.spiza.ui.screens.app

import androidx.lifecycle.ViewModel
import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

data class AppState(
    val themeMode: ThemeMode = ThemeMode.System,
    val language: AppLanguage = AppLanguage.Default,
    val isSignedIn: Boolean = false,
)

/** Exposes app-wide preferences (theme, language) applied at the root of the UI. */
abstract class AppViewModel : ViewModel() {
    abstract fun state(): StateFlow<AppState>
}
