package com.mzagorscak.spiza.domain.di

import com.mzagorscak.spiza.domain.usecase.AddArticleItemUseCase
import com.mzagorscak.spiza.domain.usecase.CreateHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteArticleItemUseCase
import com.mzagorscak.spiza.domain.usecase.GetCurrentUserProfileUseCase
import com.mzagorscak.spiza.domain.usecase.MoveArticleItemUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveAppLanguageUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveArticleItemsUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveHouseholdsUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveSelectedHouseholdIdUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveThemeModeUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveUserProfileUseCase
import com.mzagorscak.spiza.domain.usecase.SetAppLanguageUseCase
import com.mzagorscak.spiza.domain.usecase.SetArticleItemCheckedUseCase
import com.mzagorscak.spiza.domain.usecase.SetSelectedHouseholdIdUseCase
import com.mzagorscak.spiza.domain.usecase.SetThemeModeUseCase
import com.mzagorscak.spiza.domain.usecase.SignInUseCase
import com.mzagorscak.spiza.domain.usecase.SignOutUseCase
import com.mzagorscak.spiza.domain.usecase.UpdateArticleItemTextUseCase
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
        single { ObserveHouseholdsUseCase(repository = get()) }
        single { ObserveArticleItemsUseCase(repository = get()) }
        single { CreateHouseholdUseCase(repository = get()) }
        single { AddArticleItemUseCase(repository = get()) }
        single { UpdateArticleItemTextUseCase(repository = get()) }
        single { SetArticleItemCheckedUseCase(repository = get()) }
        single { MoveArticleItemUseCase(repository = get()) }
        single { DeleteArticleItemUseCase(repository = get()) }
        single { ObserveSelectedHouseholdIdUseCase(repository = get()) }
        single { SetSelectedHouseholdIdUseCase(repository = get()) }
    }
