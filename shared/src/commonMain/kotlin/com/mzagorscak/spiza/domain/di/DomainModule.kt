package com.mzagorscak.spiza.domain.di

import com.mzagorscak.spiza.domain.usecase.GetCurrentUserProfileUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveAppLanguageUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveThemeModeUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveUserProfileUseCase
import com.mzagorscak.spiza.domain.usecase.SetAppLanguageUseCase
import com.mzagorscak.spiza.domain.usecase.SetThemeModeUseCase
import com.mzagorscak.spiza.domain.usecase.SignInUseCase
import com.mzagorscak.spiza.domain.usecase.SignOutUseCase
import org.koin.dsl.module

val domainModule =
    module {
        single { GetCurrentUserProfileUseCase(repository = get()) }
        single { ObserveUserProfileUseCase(repository = get()) }
        single { ObserveThemeModeUseCase(repository = get()) }
        single { SetThemeModeUseCase(repository = get()) }
        single { ObserveAppLanguageUseCase(repository = get()) }
        single { SetAppLanguageUseCase(repository = get()) }
        single { SignInUseCase(repository = get()) }
        single { SignOutUseCase(repository = get()) }
    }
