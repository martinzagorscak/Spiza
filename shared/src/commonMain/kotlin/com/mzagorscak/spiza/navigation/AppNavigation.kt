package com.mzagorscak.spiza.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.mzagorscak.spiza.auth.AuthService
import com.mzagorscak.spiza.ui.screens.landing.LandingScreen
import com.mzagorscak.spiza.ui.screens.landing.LandingViewModel
import com.mzagorscak.spiza.ui.screens.login.LoginScreen
import com.mzagorscak.spiza.ui.screens.login.LoginViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavigation() {
    val authService = koinInject<AuthService>()
    val user by authService.authState.collectAsState(initial = authService.currentUser)
    val startRoute: Route = if (user == null) Route.Login else Route.Landing

    val backStack =
        rememberNavBackStack(
            configuration = SavedStateConfiguration { serializersModule = routeSerializersModule },
            startRoute,
        )

    // Auth is the source of truth for the root destination: reset the stack when it changes.
    LaunchedEffect(startRoute) {
        if (backStack.lastOrNull() != startRoute) {
            backStack.clear()
            backStack.add(startRoute)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider<NavKey> {
                entry<Route.Login> {
                    val viewModel = koinViewModel<LoginViewModel>()
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    LoginScreen(
                        state = state,
                        onSignInWithGoogle = viewModel::onSignInWithGoogleClicked,
                    )
                }
                entry<Route.Landing> {
                    val viewModel = koinViewModel<LandingViewModel>()
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    LandingScreen(
                        state = state,
                        onSignOut = viewModel::onSignOutClicked,
                    )
                }
            },
    )
}
