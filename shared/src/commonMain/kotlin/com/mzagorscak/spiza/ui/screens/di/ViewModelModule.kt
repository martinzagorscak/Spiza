package com.mzagorscak.spiza.ui.screens.di

import com.mzagorscak.spiza.ui.screens.app.AppViewModel
import com.mzagorscak.spiza.ui.screens.app.AppViewModelImpl
import com.mzagorscak.spiza.ui.screens.login.LoginViewModel
import com.mzagorscak.spiza.ui.screens.login.LoginViewModelImpl
import com.mzagorscak.spiza.ui.screens.settings.SettingsViewModel
import com.mzagorscak.spiza.ui.screens.settings.SettingsViewModelImpl
import com.mzagorscak.spiza.ui.screens.shoppingList.ShoppingListViewModel
import com.mzagorscak.spiza.ui.screens.shoppingList.ShoppingListViewModelImpl
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel<AppViewModel> {
        AppViewModelImpl(
            observeThemeMode = get(),
            observeAppLanguage = get(),
            observeUserProfile = get(),
            getCurrentUserProfile = get(),
        )
    }
    viewModel<LoginViewModel> { LoginViewModelImpl(signIn = get()) }
    viewModel<ShoppingListViewModel> { ShoppingListViewModelImpl() }
    viewModel<SettingsViewModel> {
        SettingsViewModelImpl(
            observeUserProfile = get(),
            observeThemeMode = get(),
            observeAppLanguage = get(),
            setThemeMode = get(),
            setAppLanguage = get(),
            signOut = get(),
        )
    }
}
