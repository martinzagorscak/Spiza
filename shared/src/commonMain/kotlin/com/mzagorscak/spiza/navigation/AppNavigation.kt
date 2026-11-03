package com.mzagorscak.spiza.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.mzagorscak.spiza.permissions.rememberPlatformPermissionsController
import com.mzagorscak.spiza.ui.components.common.performHapticFeedback
import com.mzagorscak.spiza.ui.screens.common.MainTab
import com.mzagorscak.spiza.ui.screens.createHousehold.CreateHouseholdCallbacks
import com.mzagorscak.spiza.ui.screens.createHousehold.CreateHouseholdScreen
import com.mzagorscak.spiza.ui.screens.createHousehold.CreateHouseholdViewModel
import com.mzagorscak.spiza.ui.screens.createHousehold.HouseholdScenario
import com.mzagorscak.spiza.ui.screens.householdManagement.HouseholdManagementScreen
import com.mzagorscak.spiza.ui.screens.householdManagement.HouseholdManagementScreenCallbacks
import com.mzagorscak.spiza.ui.screens.householdManagement.HouseholdManagementViewModel
import com.mzagorscak.spiza.ui.screens.login.LoginCallbacks
import com.mzagorscak.spiza.ui.screens.login.LoginScreen
import com.mzagorscak.spiza.ui.screens.login.LoginViewModel
import com.mzagorscak.spiza.ui.screens.loyaltyCards.LoyaltyCardSharedTransition
import com.mzagorscak.spiza.ui.screens.loyaltyCards.LoyaltyCardsScreen
import com.mzagorscak.spiza.ui.screens.loyaltyCards.LoyaltyCardsScreenCallbacks
import com.mzagorscak.spiza.ui.screens.loyaltyCards.LoyaltyCardsViewModel
import com.mzagorscak.spiza.ui.screens.loyaltyCards.add.AddLoyaltyCardScreen
import com.mzagorscak.spiza.ui.screens.loyaltyCards.add.AddLoyaltyCardScreenCallbacks
import com.mzagorscak.spiza.ui.screens.loyaltyCards.add.AddLoyaltyCardViewModel
import com.mzagorscak.spiza.ui.screens.loyaltyCards.details.LoyaltyCardDetailsScreen
import com.mzagorscak.spiza.ui.screens.loyaltyCards.details.LoyaltyCardDetailsScreenCallbacks
import com.mzagorscak.spiza.ui.screens.loyaltyCards.details.LoyaltyCardDetailsViewModel
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
import org.koin.core.parameter.parametersOf

@Composable
fun AppNavigation(isSignedIn: Boolean) {
    val startRoute: Route = if (isSignedIn) Route.ShoppingList else Route.Login
    val hapticFeedback = LocalHapticFeedback.current

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

    SharedTransitionLayout {
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
                                    onSignInWithGoogle = {
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.signInWithGoogle()
                                    },
                                    onLoginErrorShown = viewModel::onLoginErrorShown,
                                )
                            },
                        )
                    }
                    entry<Route.CreateHousehold> { key ->
                        val scenario = remember(key) {
                            if (key.householdId != null && key.householdName != null) {
                                HouseholdScenario.Edit(
                                    householdId = key.householdId,
                                    householdName = key.householdName,
                                    householdDescription = key.householdDescription.orEmpty(),
                                )
                            } else {
                                HouseholdScenario.Create
                            }
                        }
                        val viewModel = koinViewModel<CreateHouseholdViewModel>(parameters = { parametersOf(scenario) })
                        val state by viewModel.state().collectAsStateWithLifecycle()
                        CreateHouseholdScreen(
                            state = state,
                            callbacks = remember(key) {
                                CreateHouseholdCallbacks(
                                    onBackClick = {
                                        performHapticFeedback(hapticFeedback)
                                        backStack.popIfTop(key)
                                    },
                                    onNameChange = viewModel::onNameChange,
                                    onDescriptionChange = viewModel::onDescriptionChange,
                                    onCreateClick = {
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.createHousehold()
                                    },
                                    onCreated = { backStack.popIfTop(key) },
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
                                    onSettingsClick = {
                                        performHapticFeedback(hapticFeedback)
                                        backStack.add(Route.Settings)
                                    },
                                    onTabClick = { tab ->
                                        performHapticFeedback(hapticFeedback)
                                        backStack.switchTab(tab)
                                    },
                                    onOtherHouseholdClick = viewModel::onHouseholdSelected,
                                    onCreateHouseholdClick = {
                                        performHapticFeedback(hapticFeedback)
                                        backStack.add(Route.CreateHousehold())
                                    },
                                    onRetryClick = {
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.retry()
                                    },
                                    onAddItem = viewModel::onAddItem,
                                    onItemTextChange = viewModel::onItemTextChange,
                                    onItemCheckedChange = { itemId, isChecked ->
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.onItemCheckedChange(itemId = itemId, isChecked = isChecked)
                                    },
                                    onItemMoved = viewModel::onItemMoved,
                                    onItemDelete = { itemId ->
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.onItemDelete(itemId = itemId)
                                    },
                                    onCheckedItemsCollapse = {
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.onCheckedItemsCollapse()
                                    },
                                    showClearCheckedItemsConfirmationDialog = {
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.showClearCheckedItemsConfirmationDialog()
                                    },
                                    onDismissDialog = viewModel::dismissDialog,
                                    clearCheckedItems = {
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.clearCheckedItems()
                                    },
                                    onWriteErrorShown = viewModel::onErrorShown,
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
                                    onBackClick = {
                                        performHapticFeedback(hapticFeedback)
                                        backStack.popIfTop(Route.Settings)
                                    },
                                    onCreateJoinKeyClick = {
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.createJoinKey()
                                    },
                                    onRefreshJoinKeyClick = {
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.refreshJoinKey()
                                    },
                                    onThemeModeSelected = { themeMode ->
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.onThemeModeSelected(mode = themeMode)
                                    },
                                    onLanguageSelected = { language ->
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.onLanguageSelected(language = language)
                                    },
                                    onHouseholdManagementClick = {
                                        performHapticFeedback(hapticFeedback)
                                        backStack.add(Route.HouseholdManagement)
                                    },
                                    onShoppingItemsSavingClick = {
                                        performHapticFeedback(hapticFeedback)
                                        backStack.add(Route.SuggestionsSavingSettings)
                                    },
                                    onSignOutClick = {
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.onSignOut()
                                    },
                                    onErrorShown = viewModel::onErrorShown,
                                )
                            },
                        )
                    }
                    entry<Route.HouseholdManagement> {
                        val viewModel = koinViewModel<HouseholdManagementViewModel>()
                        val state by viewModel.state().collectAsStateWithLifecycle()
                        val dialogInput by viewModel.dialogInput().collectAsStateWithLifecycle()
                        HouseholdManagementScreen(
                            state = state,
                            dialogInput = dialogInput,
                            callbacks = remember {
                                HouseholdManagementScreenCallbacks(
                                    onBackClick = {
                                        performHapticFeedback(hapticFeedback)
                                        backStack.popIfTop(Route.HouseholdManagement)
                                    },
                                    onCreateHousehold = {
                                        performHapticFeedback(hapticFeedback)
                                        backStack.add(Route.CreateHousehold())
                                    },
                                    onEditHousehold = { household ->
                                        performHapticFeedback(hapticFeedback)
                                        backStack.add(
                                            Route.CreateHousehold(
                                                householdId = household.id,
                                                householdName = household.name,
                                                householdDescription = household.description,
                                            ),
                                        )
                                    },
                                    showAddMemberToHouseholdsDialog = { householdId ->
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.showAddMemberToHouseholdDialog(householdId = householdId)
                                    },
                                    onAddMember = { householdId ->
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.addMemberToHousehold(householdId = householdId)
                                    },
                                    showRemoveMemberFromHouseholdDialog = { household, member ->
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.showRemoveMemberFromHouseholdDialog(household = household, member = member)
                                    },
                                    onRemoveMember = { householdId, memberId, householdOwnerId ->
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.removeMemberFromHousehold(
                                            householdId = householdId,
                                            memberId = memberId,
                                            householdOwnerId = householdOwnerId,
                                        )
                                    },
                                    showDeleteHouseholdDialog = { household ->
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.showDeleteHouseholdDialog(household = household)
                                    },
                                    onDeleteHousehold = { householdId, householdOwnerId ->
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.deleteHousehold(
                                            householdId = householdId,
                                            householdOwnerId = householdOwnerId,
                                        )
                                    },
                                    onDialogJoinKeyInputChanged = viewModel::onDialogJoinKeyInputChanged,
                                    onDismissDialog = viewModel::dismissDialog,
                                    onUserNotFoundErrorShown = viewModel::onErrorShown,
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
                                    onBackClick = {
                                        performHapticFeedback(hapticFeedback)
                                        backStack.popIfTop(Route.SuggestionsSavingSettings)
                                    },
                                    onToggleSavingEnabled = { enabled ->
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.onToggleSavingEnabled(enabled = enabled)
                                    },
                                    onSortingTypeChange = viewModel::setSortingType,
                                    onRemoveSuggestion = { suggestionId ->
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.removeSuggestion(suggestionId = suggestionId)
                                    },
                                    showRemoveAllSuggestionsConfirmationDialog = {
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.showRemoveAllSuggestionsConfirmationDialog()
                                    },
                                    showAddSuggestionDialog = {
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.showAddSuggestionDialog()
                                    },
                                    onDialogInputChanged = viewModel::onDialogInputChanged,
                                    onAddSuggestion = {
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.addSuggestion()
                                    },
                                    onDismissDialog = viewModel::dismissDialog,
                                    removeAllSuggestions = {
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.removeAllSuggestions()
                                    },
                                )
                            },
                        )
                    }
                    entry<Route.LoyaltyCards>(metadata = tabTransition) {
                        val viewModel = koinViewModel<LoyaltyCardsViewModel>()
                        val state by viewModel.state().collectAsStateWithLifecycle()
                        LoyaltyCardsScreen(
                            state = state,
                            sharedTransition = LoyaltyCardSharedTransition(
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                            ),
                            callbacks = remember {
                                LoyaltyCardsScreenCallbacks(
                                    onTabClick = { tab ->
                                        performHapticFeedback(hapticFeedback)
                                        backStack.switchTab(tab)
                                    },
                                    onAddLoyaltyCardClick = {
                                        performHapticFeedback(hapticFeedback)
                                        backStack.add(Route.AddLoyaltyCard)
                                    },
                                    onCardClick = { cardId ->
                                        performHapticFeedback(hapticFeedback)
                                        backStack.add(Route.LoyaltyCardDetails(cardId))
                                    },
                                )
                            },
                        )
                    }
                    entry<Route.LoyaltyCardDetails> { key ->
                        val viewModel = koinViewModel<LoyaltyCardDetailsViewModel>(parameters = { parametersOf(key.cardId) })
                        val state by viewModel.state().collectAsStateWithLifecycle()
                        LoyaltyCardDetailsScreen(
                            state = state,
                            sharedTransition = LoyaltyCardSharedTransition(
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                            ),
                            callbacks = remember(key) {
                                LoyaltyCardDetailsScreenCallbacks(
                                    onBackClick = {
                                        performHapticFeedback(hapticFeedback)
                                        backStack.popIfTop(key)
                                    },
                                    onDeleteClick = {
                                        performHapticFeedback(hapticFeedback)
                                        viewModel.deleteCard()
                                        backStack.popIfTop(key)
                                    },
                                )
                            },
                        )
                    }
                    entry<Route.AddLoyaltyCard> {
                        val controller = rememberPlatformPermissionsController()
                        val viewModel = koinViewModel<AddLoyaltyCardViewModel>(parameters = { parametersOf(controller) })
                        val state by viewModel.state().collectAsStateWithLifecycle()
                        LaunchedEffect(controller) {
                            viewModel.checkPermission()
                        }
                        AddLoyaltyCardScreen(
                            state = state,
                            callbacks = remember {
                                AddLoyaltyCardScreenCallbacks(
                                    onBackClick = {
                                        performHapticFeedback(hapticFeedback)
                                        backStack.popIfTop(Route.AddLoyaltyCard)
                                    },
                                    onCardNameChange = viewModel::onCardNameChange,
                                    onColorSelect = viewModel::onColorSelect,
                                    onInputTypeChange = viewModel::onInputTypeChange,
                                    onCodeChange = viewModel::onCodeChange,
                                    onFormatChange = viewModel::onFormatChange,
                                    onSaveClick = {
                                        viewModel.onSaveClick()
                                        performHapticFeedback(hapticFeedback)
                                        backStack.popIfTop(Route.AddLoyaltyCard)
                                    },
                                    onRequestPermission = viewModel::requestPermission,
                                    onOpenSettings = viewModel::openSettings,
                                )
                            },
                        )
                    }
                },
        )
    }
}

// Switching tabs crossfades instead of using the push/pop slide, so the bottom bar stays in place.
private val tabTransition =
    NavDisplay.transitionSpec { fadeIn() togetherWith fadeOut() } +
        NavDisplay.popTransitionSpec { fadeIn() togetherWith fadeOut() } +
        NavDisplay.predictivePopTransitionSpec { fadeIn() togetherWith fadeOut() }

private fun NavBackStack<NavKey>.popIfTop(route: Route) {
    if (size > 1 && lastOrNull() == route) {
        removeLastOrNull()
    }
}

// Loyalty cards sit on top of the shopping list instead of replacing it, so system back on that tab returns to the start tab.
private fun NavBackStack<NavKey>.switchTab(tab: MainTab) {
    when (tab) {
        MainTab.ShoppingList -> popIfTop(Route.LoyaltyCards)
        MainTab.LoyaltyCards -> if (lastOrNull() == Route.ShoppingList) add(Route.LoyaltyCards)
    }
}
