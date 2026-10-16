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
import com.mzagorscak.spiza.ui.screens.createHousehold.CreateHouseholdCallbacks
import com.mzagorscak.spiza.ui.screens.createHousehold.CreateHouseholdScreen
import com.mzagorscak.spiza.ui.screens.createHousehold.CreateHouseholdViewModel
import com.mzagorscak.spiza.ui.screens.login.LoginCallbacks
import com.mzagorscak.spiza.ui.screens.login.LoginScreen
import com.mzagorscak.spiza.ui.screens.login.LoginViewModel
import com.mzagorscak.spiza.ui.screens.settings.SettingsScreen
import com.mzagorscak.spiza.ui.screens.settings.SettingsScreenCallbacks
import com.mzagorscak.spiza.ui.screens.settings.SettingsViewModel
import com.mzagorscak.spiza.ui.screens.shoppingList.ShoppingListScreen
import com.mzagorscak.spiza.ui.screens.shoppingList.ShoppingListScreenCallbacks
import com.mzagorscak.spiza.ui.screens.shoppingList.ShoppingListViewModel
import com.mzagorscak.spiza.ui.screens.suggestionSaving.SuggestionSavingSettingViewModel
import com.mzagorscak.spiza.ui.screens.suggestionSaving.SuggestionSavingSettingsScreen
import com.mzagorscak.spiza.ui.screens.suggestionSaving.SuggestionSavingSettingsScreenCallbacks
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
                                onLoginErrorShown = viewModel::onLoginErrorShown,
                            )
                        },
                    )
                }
                entry<Route.CreateHousehold> {
                    val viewModel = koinViewModel<CreateHouseholdViewModel>()
                    val state by viewModel.state().collectAsStateWithLifecycle()
                    CreateHouseholdScreen(
                        state = state,
                        callbacks = remember {
                            CreateHouseholdCallbacks(
                                onBackClick = { backStack.popIfTop(Route.CreateHousehold) },
                                onNameChange = viewModel::onNameChange,
                                onDescriptionChange = viewModel::onDescriptionChange,
                                onCreateClick = viewModel::onCreateClicked,
                                onCreated = { backStack.popIfTop(Route.CreateHousehold) },
                            )
                        },
                    )
                }
                entry<Route.ShoppingList> {
                    val viewModel = koinViewModel<ShoppingListViewModel>()
                    val headerState by viewModel.headerState().collectAsStateWithLifecycle()
                    val bodyState by viewModel.bodyState().collectAsStateWithLifecycle()
                    ShoppingListScreen(
                        headerState = headerState,
                        bodyState = bodyState,
                        callbacks = remember {
                            ShoppingListScreenCallbacks(
                                onSettingsClick = { backStack.add(Route.Settings) },
                                onOtherHouseholdClick = viewModel::onHouseholdSelected,
                                onCreateHouseholdClick = { backStack.add(Route.CreateHousehold) },
                                onRetryClick = viewModel::onRetryClicked,
                                onAddItem = viewModel::onAddItem,
                                onItemTextChange = viewModel::onItemTextChange,
                                onItemCheckedChange = viewModel::onItemCheckedChange,
                                onItemMoved = viewModel::onItemMoved,
                                onCheckedItemsCollapse = viewModel::onCheckedItemsCollapse,
                                showClearCheckedItemsConfirmationDialog = viewModel::showClearCheckedItemsConfirmationDialog,
                                onDismissDialog = viewModel::dismissDialog,
                                clearCheckedItems = viewModel::clearCheckedItems,
                                onWriteErrorShown = viewModel::onWriteErrorShown,
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
                                onBackClick = { backStack.popIfTop(Route.Settings) },
                                onThemeModeSelected = viewModel::onThemeModeSelected,
                                onLanguageSelected = viewModel::onLanguageSelected,
                                onHouseholdManagementClick = {},
                                onShoppingItemsSavingClick = { backStack.add(Route.SuggestionsSavingSettings) },
                                onSignOutClick = viewModel::onSignOutClicked,
                            )
                        },
                    )
                }
                entry<Route.SuggestionsSavingSettings> {
                    val viewModel = koinViewModel<SuggestionSavingSettingViewModel>()
                    val state by viewModel.state().collectAsStateWithLifecycle()
                    SuggestionSavingSettingsScreen(
                        state = state,
                        callbacks = remember {
                            SuggestionSavingSettingsScreenCallbacks(
                                onBackClick = { backStack.popIfTop(Route.SuggestionsSavingSettings) },
                                onToggleSavingEnabled = viewModel::onToggleSavingEnabled,
                                onSortingTypeChange = viewModel::setSortingType,
                                onRemoveSuggestion = viewModel::onRemoveSuggestion,
                                showRemoveAllSuggestionsConfirmationDialog = viewModel::showRemoveAllSuggestionsConfirmationDialog,
                                onDismissDialog = viewModel::dismissDialog,
                                removeAllSuggestions = viewModel::removeAllSuggestions,
                            )
                        },
                    )
                }
            },
    )
}

private fun androidx.navigation3.runtime.NavBackStack<NavKey>.popIfTop(route: Route) {
    if (size > 1 && lastOrNull() == route) {
        removeLastOrNull()
    }
}
