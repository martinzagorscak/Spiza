package com.mzagorscak.spiza.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.mzagorscak.spiza.ui.screens.login.LoginCallbacks
import com.mzagorscak.spiza.ui.screens.login.LoginScreen
import com.mzagorscak.spiza.ui.screens.login.LoginViewModel
import com.mzagorscak.spiza.ui.screens.settings.SettingsScreen
import com.mzagorscak.spiza.ui.screens.settings.SettingsScreenCallbacks
import com.mzagorscak.spiza.ui.screens.settings.SettingsViewModel
import com.mzagorscak.spiza.ui.screens.shoppingList.ShoppingListScreen
import com.mzagorscak.spiza.ui.screens.shoppingList.ShoppingListScreenCallbacks
import com.mzagorscak.spiza.ui.screens.shoppingList.ShoppingListViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavigation(isSignedIn: Boolean) {
    val startRoute: Route = if (isSignedIn) Route.ShoppingList else Route.Login

    val backStack =
        rememberNavBackStack(
            configuration = SavedStateConfiguration { serializersModule = routeSerializersModule },
            startRoute,
        )

    // Auth is the source of truth for the root destination: reset the stack only when the root no longer matches it,
    // so a restored back stack (e.g. after a configuration change) is kept.
    LaunchedEffect(startRoute) {
        if (backStack.firstOrNull() != startRoute) {
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
                    val state by viewModel.state().collectAsStateWithLifecycle()
                    LoginScreen(
                        state = state,
                        callbacks = remember {
                            LoginCallbacks(
                                onSignInWithGoogle = viewModel::onSignInWithGoogleClicked,
                            )
                        },
                    )
                }
                entry<Route.ShoppingList> {
                    val viewModel = koinViewModel<ShoppingListViewModel>()
                    val headerState by viewModel.headerState().collectAsStateWithLifecycle()
                    ShoppingListScreen(
                        headerState = headerState,
                        callbacks = remember {
                            ShoppingListScreenCallbacks(
                                onSettingsClick = { backStack.add(Route.Settings) },
                            )
                        },
                    )
                }
                entry<Route.Settings> {
                    val viewModel = koinViewModel<SettingsViewModel>()
                    val state by viewModel.state().collectAsStateWithLifecycle()
                    SettingsScreen(
                        state = state,
                        callbacks = remember {
                            SettingsScreenCallbacks(
                                onBackClick = { backStack.removeLastOrNull() },
                                onThemeModeSelected = viewModel::onThemeModeSelected,
                                onLanguageSelected = viewModel::onLanguageSelected,
                                onHouseholdManagementClick = {},
                                onShoppingItemsCachingClick = {},
                                onSignOutClick = viewModel::onSignOutClicked,
                            )
                        },
                    )
                }
            },
    )
}
