package com.mzagorscak.spiza

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mzagorscak.spiza.domain.model.ThemeMode
import com.mzagorscak.spiza.navigation.AppNavigation
import com.mzagorscak.spiza.ui.components.animatable.CircularRevealTransition
import com.mzagorscak.spiza.ui.locale.ProvideAppLanguage
import com.mzagorscak.spiza.ui.screens.app.AppViewModel
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App() {
    val viewModel = koinViewModel<AppViewModel>()
    val state by viewModel.state().collectAsStateWithLifecycle()
    val darkTheme =
        when (state.themeMode) {
            ThemeMode.System -> isSystemInDarkTheme()
            ThemeMode.Light -> false
            ThemeMode.Dark -> true
        }

    ProvideAppLanguage(state.language) {
        CircularRevealTransition(targetState = darkTheme) { isDarkTheme ->
            TheAppTheme(darkTheme = isDarkTheme) {
                AppNavigation(
                    isSignedIn = state.isSignedIn,
                    isOnboardingCompleted = state.isOnboardingCompleted,
                )
            }
        }
    }
}
