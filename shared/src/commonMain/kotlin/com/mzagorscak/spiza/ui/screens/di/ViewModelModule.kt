package com.mzagorscak.spiza.ui.screens.di

import com.mzagorscak.spiza.ui.screens.landing.LandingViewModel
import com.mzagorscak.spiza.ui.screens.landing.LandingViewModelImpl
import com.mzagorscak.spiza.ui.screens.login.LoginViewModel
import com.mzagorscak.spiza.ui.screens.login.LoginViewModelImpl
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel<LoginViewModel> { LoginViewModelImpl(authService = get()) }
    viewModel<LandingViewModel> { LandingViewModelImpl(authService = get()) }
}
