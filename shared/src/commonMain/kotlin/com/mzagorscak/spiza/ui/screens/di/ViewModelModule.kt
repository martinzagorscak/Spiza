package com.mzagorscak.spiza.ui.screens.di

import com.mzagorscak.spiza.ui.screens.app.AppViewModel
import com.mzagorscak.spiza.ui.screens.app.AppViewModelImpl
import com.mzagorscak.spiza.ui.screens.createHousehold.CreateHouseholdViewModel
import com.mzagorscak.spiza.ui.screens.createHousehold.CreateHouseholdViewModelImpl
import com.mzagorscak.spiza.ui.screens.createHousehold.HouseholdScenario
import com.mzagorscak.spiza.ui.screens.householdManagement.HouseholdManagementViewModel
import com.mzagorscak.spiza.ui.screens.householdManagement.HouseholdManagementViewModelImpl
import com.mzagorscak.spiza.ui.screens.login.LoginViewModel
import com.mzagorscak.spiza.ui.screens.login.LoginViewModelImpl
import com.mzagorscak.spiza.ui.screens.loyaltyCards.LoyaltyCardsViewModel
import com.mzagorscak.spiza.ui.screens.loyaltyCards.LoyaltyCardsViewModelImpl
import com.mzagorscak.spiza.ui.screens.loyaltyCards.add.AddLoyaltyCardViewModel
import com.mzagorscak.spiza.ui.screens.loyaltyCards.add.AddLoyaltyCardViewModelImpl
import com.mzagorscak.spiza.ui.screens.loyaltyCards.details.LoyaltyCardDetailsViewModel
import com.mzagorscak.spiza.ui.screens.loyaltyCards.details.LoyaltyCardDetailsViewModelImpl
import com.mzagorscak.spiza.ui.screens.settings.SettingsViewModel
import com.mzagorscak.spiza.ui.screens.settings.SettingsViewModelImpl
import com.mzagorscak.spiza.ui.screens.shoppingList.ShoppingListViewModel
import com.mzagorscak.spiza.ui.screens.shoppingList.ShoppingListViewModelImpl
import com.mzagorscak.spiza.ui.screens.suggestionSaving.SuggestionSavingSettingViewModel
import com.mzagorscak.spiza.ui.screens.suggestionSaving.SuggestionSavingSettingViewModelImpl
import dev.icerock.moko.permissions.PermissionsController
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModel<AppViewModel> {
        AppViewModelImpl(
            observeThemeMode = get(),
            observeAppLanguage = get(),
            observeUserProfile = get(),
            getCurrentThemeMode = get(),
            getCurrentAppLanguage = get(),
            getCurrentUserProfile = get(),
            crashLogger = get(),
        )
    }
    viewModel<LoginViewModel> { LoginViewModelImpl(signIn = get(), crashLogger = get()) }
    viewModel<CreateHouseholdViewModel> { (scenario: HouseholdScenario) ->
        CreateHouseholdViewModelImpl(
            scenario = scenario,
            createHousehold = get(),
            updateHousehold = get(),
            crashLogger = get(),
        )
    }
    viewModel<ShoppingListViewModel> {
        ShoppingListViewModelImpl(
            observeHouseholdSelection = get(),
            observeItems = get(),
            setSelectedHouseholdId = get(),
            canUserCreateHousehold = get(),
            getCurrentUserProfile = get(),
            canAddItemToHousehold = get(),
            addItem = get(),
            updateItemText = get(),
            setItemChecked = get(),
            moveItem = get(),
            deleteItem = get(),
            addOrUpdateSuggestion = get(),
            clearCheckedItems = get(),
            observeSuggestionsSavingEnabled = get(),
            observeSuggestions = get(),
            crashLogger = get(),
        )
    }
    viewModel<SettingsViewModel> {
        SettingsViewModelImpl(
            observeUserProfile = get(),
            canUserJoinHousehold = get(),
            createHouseholdJoinKey = get(),
            getHouseholdJoinKey = get(),
            refreshHouseholdJoinKey = get(),
            observeThemeMode = get(),
            observeAppLanguage = get(),
            setThemeMode = get(),
            setAppLanguage = get(),
            signOut = get(),
            crashLogger = get(),
        )
    }
    viewModel<HouseholdManagementViewModel> {
        HouseholdManagementViewModelImpl(
            getCurrentUserProfileUseCase = get(),
            observeHouseholds = get(),
            addMemberToHousehold = get(),
            removeMemberFromHouseholdUseCase = get(),
            deleteHouseholdUseCase = get(),
            canUserCreateHousehold = get(),
            canAddMemberToHousehold = get(),
            crashLogger = get(),
        )
    }
    viewModel<SuggestionSavingSettingViewModel> {
        SuggestionSavingSettingViewModelImpl(
            observeSuggestionsSavingEnabled = get(),
            observeSuggestions = get(),
            setSuggestionsSavingEnabled = get(),
            deleteSuggestionUseCase = get(),
            deleteAllSuggestionsUseCase = get(),
            addOrUpdateSuggestionUseCase = get(),
            crashLogger = get(),
        )
    }
    viewModel<AddLoyaltyCardViewModel> { (controller: PermissionsController) ->
        AddLoyaltyCardViewModelImpl(
            addLoyaltyCardUseCase = get(),
            validateLoyaltyCardCodeUseCase = get(),
            canEnterLoyaltyCardCodeUseCase = get(),
            detectLoyaltyCardFormatUseCase = get(),
            normalizeScannedLoyaltyCardCodeUseCase = get(),
            checkPermissionUseCase = get { parametersOf(controller) },
            requestPermissionUseCase = get { parametersOf(controller) },
            openAppSettingsUseCase = get { parametersOf(controller) },
            crashLogger = get(),
        )
    }
    viewModel<LoyaltyCardsViewModel> {
        LoyaltyCardsViewModelImpl(
            observeLoyaltyCardsUseCase = get(),
            canUseAppShortcutsUseCase = get(),
            toggleLoyaltyCardShortcutUseCase = get(),
            crashLogger = get(),
        )
    }
    viewModel<LoyaltyCardDetailsViewModel> { (cardId: Long) ->
        LoyaltyCardDetailsViewModelImpl(
            cardId = cardId,
            observeLoyaltyCardByIdUseCase = get(),
            deleteLoyaltyCardUseCase = get(),
            crashLogger = get(),
        )
    }
}
