package com.mzagorscak.spiza.domain.di

import com.mzagorscak.spiza.domain.constraints.ConstraintsManager
import com.mzagorscak.spiza.domain.constraints.ConstraintsManagerImpl
import com.mzagorscak.spiza.domain.usecase.AddArticleItemUseCase
import com.mzagorscak.spiza.domain.usecase.AddMemberToHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.AddOrUpdateSuggestionUseCase
import com.mzagorscak.spiza.domain.usecase.CanAddItemToHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.CanAddMemberToHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.CanUserCreateHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.CanUserJoinHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.ClearCheckedItemsUseCase
import com.mzagorscak.spiza.domain.usecase.CreateHouseholdJoinKeyUseCase
import com.mzagorscak.spiza.domain.usecase.CreateHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteAllSuggestionsUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteArticleItemUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteSuggestionUseCase
import com.mzagorscak.spiza.domain.usecase.GetCurrentAppLanguageUseCase
import com.mzagorscak.spiza.domain.usecase.GetCurrentThemeModeUseCase
import com.mzagorscak.spiza.domain.usecase.GetCurrentUserProfileUseCase
import com.mzagorscak.spiza.domain.usecase.GetHouseholdJoinKeyUseCase
import com.mzagorscak.spiza.domain.usecase.GetUsersUseCase
import com.mzagorscak.spiza.domain.usecase.MoveArticleItemUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveAppLanguageUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveArticleItemsUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveHouseholdsUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveSelectedHouseholdIdUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsSavingEnabledUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveThemeModeUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveUserProfileUseCase
import com.mzagorscak.spiza.domain.usecase.RemoveMemberFromHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.SetAppLanguageUseCase
import com.mzagorscak.spiza.domain.usecase.SetArticleItemCheckedUseCase
import com.mzagorscak.spiza.domain.usecase.SetSelectedHouseholdIdUseCase
import com.mzagorscak.spiza.domain.usecase.SetSuggestionsSavingEnabledUseCase
import com.mzagorscak.spiza.domain.usecase.SetThemeModeUseCase
import com.mzagorscak.spiza.domain.usecase.SignInUseCase
import com.mzagorscak.spiza.domain.usecase.SignOutUseCase
import com.mzagorscak.spiza.domain.usecase.UpdateArticleItemTextUseCase
import org.koin.dsl.module

val domainModule =
    module {
        single<ConstraintsManager> { ConstraintsManagerImpl }
        single { GetCurrentUserProfileUseCase(repository = get()) }
        single { ObserveUserProfileUseCase(repository = get()) }
        single { GetUsersUseCase(repository = get()) }
        single { ObserveThemeModeUseCase(repository = get()) }
        single { GetCurrentThemeModeUseCase(repository = get()) }
        single { SetThemeModeUseCase(repository = get()) }
        single { ObserveAppLanguageUseCase(repository = get()) }
        single { GetCurrentAppLanguageUseCase(repository = get()) }
        single { SetAppLanguageUseCase(repository = get()) }
        single { ObserveSuggestionsSavingEnabledUseCase(repository = get()) }
        single { SetSuggestionsSavingEnabledUseCase(repository = get()) }
        single { SignInUseCase(repository = get()) }
        single { SignOutUseCase(repository = get()) }
        single { CreateHouseholdJoinKeyUseCase(repository = get()) }
        single { GetHouseholdJoinKeyUseCase(repository = get()) }
        single { AddMemberToHouseholdUseCase(repository = get()) }
        single { RemoveMemberFromHouseholdUseCase(repository = get()) }
        single { DeleteHouseholdUseCase(repository = get()) }
        single { ObserveHouseholdsUseCase(repository = get()) }
        single { ObserveArticleItemsUseCase(repository = get()) }
        single { CreateHouseholdUseCase(repository = get()) }
        single { AddArticleItemUseCase(repository = get()) }
        single { UpdateArticleItemTextUseCase(repository = get()) }
        single { SetArticleItemCheckedUseCase(repository = get()) }
        single { MoveArticleItemUseCase(repository = get()) }
        single { DeleteArticleItemUseCase(repository = get()) }
        single { ClearCheckedItemsUseCase(repository = get()) }
        single { ObserveSelectedHouseholdIdUseCase(repository = get()) }
        single { SetSelectedHouseholdIdUseCase(repository = get()) }
        single { CanUserCreateHouseholdUseCase(constraintsManager = get()) }
        single { CanUserJoinHouseholdUseCase(repository = get(), constraintsManager = get()) }
        single { CanAddItemToHouseholdUseCase(constraintsManager = get()) }
        single { CanAddMemberToHouseholdUseCase(constraintsManager = get()) }
        single { ObserveSuggestionsUseCase(localDataSource = get()) }
        single { AddOrUpdateSuggestionUseCase(localDataSource = get()) }
        single { DeleteSuggestionUseCase(localDataSource = get()) }
        single { DeleteAllSuggestionsUseCase(localDataSource = get()) }
    }
