package com.mzagorscak.spiza.ui.screens.di

import com.mzagorscak.spiza.ui.screens.app.AppViewModel
import com.mzagorscak.spiza.ui.screens.app.AppViewModelImpl
import com.mzagorscak.spiza.ui.screens.createHousehold.CreateHouseholdViewModel
import com.mzagorscak.spiza.ui.screens.createHousehold.CreateHouseholdViewModelImpl
import com.mzagorscak.spiza.ui.screens.householdManagement.HouseholdManagementViewModel
import com.mzagorscak.spiza.ui.screens.householdManagement.HouseholdManagementViewModelImpl
import com.mzagorscak.spiza.ui.screens.login.LoginViewModel
import com.mzagorscak.spiza.ui.screens.login.LoginViewModelImpl
import com.mzagorscak.spiza.ui.screens.settings.SettingsViewModel
import com.mzagorscak.spiza.ui.screens.settings.SettingsViewModelImpl
import com.mzagorscak.spiza.ui.screens.shoppingList.ShoppingListViewModel
import com.mzagorscak.spiza.ui.screens.shoppingList.ShoppingListViewModelImpl
import com.mzagorscak.spiza.ui.screens.suggestionSaving.SuggestionSavingSettingViewModel
import com.mzagorscak.spiza.ui.screens.suggestionSaving.SuggestionSavingSettingViewModelImpl
import org.koin.core.module.dsl.viewModel
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
        )
    }
    viewModel<LoginViewModel> { LoginViewModelImpl(signIn = get()) }
    viewModel<CreateHouseholdViewModel> {
        CreateHouseholdViewModelImpl(
            createHousehold = get(),
            crashLogger = get(),
        )
    }
    viewModel<ShoppingListViewModel> {
        ShoppingListViewModelImpl(
            observeHouseholds = get(),
            observeItems = get(),
            observeSelectedHouseholdId = get(),
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
            observeThemeMode = get(),
            observeAppLanguage = get(),
            setThemeMode = get(),
            setAppLanguage = get(),
            signOut = get(),
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
        )
    }
    viewModel<SuggestionSavingSettingViewModel> {
        SuggestionSavingSettingViewModelImpl(
            observeSuggestionsSavingEnabled = get(),
            observeSuggestions = get(),
            setSuggestionsSavingEnabled = get(),
            deleteSuggestionUseCase = get(),
            deleteAllSuggestionsUseCase = get(),
        )
    }
}
