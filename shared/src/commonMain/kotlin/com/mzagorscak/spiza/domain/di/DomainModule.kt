package com.mzagorscak.spiza.domain.di

import com.mzagorscak.spiza.domain.constraints.ConstraintsManager
import com.mzagorscak.spiza.domain.constraints.ConstraintsManagerImpl
import com.mzagorscak.spiza.domain.usecase.AddArticleItemUseCase
import com.mzagorscak.spiza.domain.usecase.AddArticleItemUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.AddLoyaltyCardUseCase
import com.mzagorscak.spiza.domain.usecase.AddLoyaltyCardUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.AddMemberToHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.AddMemberToHouseholdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.AddOrUpdateSuggestionUseCase
import com.mzagorscak.spiza.domain.usecase.AddOrUpdateSuggestionUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.CanAddItemToHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.CanAddItemToHouseholdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.CanAddMemberToHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.CanAddMemberToHouseholdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.CanUserCreateHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.CanUserCreateHouseholdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.CanUserJoinHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.CanUserJoinHouseholdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.CheckPermissionUseCase
import com.mzagorscak.spiza.domain.usecase.CheckPermissionUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ClearCheckedItemsUseCase
import com.mzagorscak.spiza.domain.usecase.ClearCheckedItemsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.CreateHouseholdJoinKeyUseCase
import com.mzagorscak.spiza.domain.usecase.CreateHouseholdJoinKeyUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.CreateHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.CreateHouseholdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.DeleteAllSuggestionsUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteAllSuggestionsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.DeleteArticleItemUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteArticleItemUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.DeleteHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteHouseholdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.DeleteLoyaltyCardUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteLoyaltyCardUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.DeleteSuggestionUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteSuggestionUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.GetCurrentAppLanguageUseCase
import com.mzagorscak.spiza.domain.usecase.GetCurrentAppLanguageUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.GetCurrentThemeModeUseCase
import com.mzagorscak.spiza.domain.usecase.GetCurrentThemeModeUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.GetCurrentUserProfileUseCase
import com.mzagorscak.spiza.domain.usecase.GetCurrentUserProfileUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.GetHouseholdJoinKeyUseCase
import com.mzagorscak.spiza.domain.usecase.GetHouseholdJoinKeyUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.GetLoyaltyCardByIdUseCase
import com.mzagorscak.spiza.domain.usecase.GetLoyaltyCardByIdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.GetUsersUseCase
import com.mzagorscak.spiza.domain.usecase.GetUsersUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.MoveArticleItemUseCase
import com.mzagorscak.spiza.domain.usecase.MoveArticleItemUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveAppLanguageUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveAppLanguageUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveArticleItemsUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveArticleItemsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveHouseholdsUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveHouseholdsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveLoyaltyCardByIdUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveLoyaltyCardByIdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveLoyaltyCardsUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveLoyaltyCardsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveSelectedHouseholdIdUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveSelectedHouseholdIdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsSavingEnabledUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsSavingEnabledUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveThemeModeUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveThemeModeUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveUserProfileUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveUserProfileUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.OpenAppSettingsUseCase
import com.mzagorscak.spiza.domain.usecase.OpenAppSettingsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.RefreshHouseholdJoinKeyUseCase
import com.mzagorscak.spiza.domain.usecase.RefreshHouseholdJoinKeyUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.RemoveMemberFromHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.RemoveMemberFromHouseholdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.RequestPermissionUseCase
import com.mzagorscak.spiza.domain.usecase.RequestPermissionUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.SetAppLanguageUseCase
import com.mzagorscak.spiza.domain.usecase.SetAppLanguageUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.SetArticleItemCheckedUseCase
import com.mzagorscak.spiza.domain.usecase.SetArticleItemCheckedUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.SetSelectedHouseholdIdUseCase
import com.mzagorscak.spiza.domain.usecase.SetSelectedHouseholdIdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.SetSuggestionsSavingEnabledUseCase
import com.mzagorscak.spiza.domain.usecase.SetSuggestionsSavingEnabledUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.SetThemeModeUseCase
import com.mzagorscak.spiza.domain.usecase.SetThemeModeUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.SignInUseCase
import com.mzagorscak.spiza.domain.usecase.SignInUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.SignOutUseCase
import com.mzagorscak.spiza.domain.usecase.SignOutUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.UpdateArticleItemTextUseCase
import com.mzagorscak.spiza.domain.usecase.UpdateArticleItemTextUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.UpdateHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.UpdateHouseholdUseCaseImpl
import org.koin.dsl.module

val domainModule =
    module {
        single<ConstraintsManager> { ConstraintsManagerImpl }

        // Settings Use Cases
        single<GetCurrentUserProfileUseCase> { GetCurrentUserProfileUseCaseImpl(repository = get()) }
        single<ObserveUserProfileUseCase> { ObserveUserProfileUseCaseImpl(repository = get()) }
        single<GetUsersUseCase> { GetUsersUseCaseImpl(repository = get()) }
        single<ObserveThemeModeUseCase> { ObserveThemeModeUseCaseImpl(repository = get()) }
        single<GetCurrentThemeModeUseCase> { GetCurrentThemeModeUseCaseImpl(repository = get()) }
        single<SetThemeModeUseCase> { SetThemeModeUseCaseImpl(repository = get()) }
        single<ObserveAppLanguageUseCase> { ObserveAppLanguageUseCaseImpl(repository = get()) }
        single<GetCurrentAppLanguageUseCase> { GetCurrentAppLanguageUseCaseImpl(repository = get()) }
        single<SetAppLanguageUseCase> { SetAppLanguageUseCaseImpl(repository = get()) }
        single<ObserveSuggestionsSavingEnabledUseCase> { ObserveSuggestionsSavingEnabledUseCaseImpl(repository = get()) }
        single<SetSuggestionsSavingEnabledUseCase> { SetSuggestionsSavingEnabledUseCaseImpl(repository = get()) }
        single<SignInUseCase> { SignInUseCaseImpl(repository = get()) }
        single<SignOutUseCase> { SignOutUseCaseImpl(repository = get()) }

        // Household Management Use Cases
        single<CreateHouseholdJoinKeyUseCase> { CreateHouseholdJoinKeyUseCaseImpl(repository = get()) }
        single<GetHouseholdJoinKeyUseCase> { GetHouseholdJoinKeyUseCaseImpl(repository = get()) }
        single<RefreshHouseholdJoinKeyUseCase> { RefreshHouseholdJoinKeyUseCaseImpl(repository = get()) }
        single<AddMemberToHouseholdUseCase> { AddMemberToHouseholdUseCaseImpl(repository = get()) }
        single<RemoveMemberFromHouseholdUseCase> { RemoveMemberFromHouseholdUseCaseImpl(repository = get()) }
        single<DeleteHouseholdUseCase> { DeleteHouseholdUseCaseImpl(repository = get()) }
        single<ObserveHouseholdsUseCase> { ObserveHouseholdsUseCaseImpl(repository = get()) }
        single<CreateHouseholdUseCase> { CreateHouseholdUseCaseImpl(repository = get()) }
        single<UpdateHouseholdUseCase> { UpdateHouseholdUseCaseImpl(repository = get()) }
        single<ObserveSelectedHouseholdIdUseCase> { ObserveSelectedHouseholdIdUseCaseImpl(repository = get()) }
        single<SetSelectedHouseholdIdUseCase> { SetSelectedHouseholdIdUseCaseImpl(repository = get()) }

        // Household Items Use Cases
        single<ObserveArticleItemsUseCase> { ObserveArticleItemsUseCaseImpl(repository = get()) }
        single<AddArticleItemUseCase> { AddArticleItemUseCaseImpl(repository = get()) }
        single<UpdateArticleItemTextUseCase> { UpdateArticleItemTextUseCaseImpl(repository = get()) }
        single<SetArticleItemCheckedUseCase> { SetArticleItemCheckedUseCaseImpl(repository = get()) }
        single<MoveArticleItemUseCase> { MoveArticleItemUseCaseImpl(repository = get()) }
        single<DeleteArticleItemUseCase> { DeleteArticleItemUseCaseImpl(repository = get()) }
        single<ClearCheckedItemsUseCase> { ClearCheckedItemsUseCaseImpl(repository = get()) }

        // Household Verification Use Cases
        single<CanUserCreateHouseholdUseCase> { CanUserCreateHouseholdUseCaseImpl(constraintsManager = get()) }
        single<CanUserJoinHouseholdUseCase> { CanUserJoinHouseholdUseCaseImpl(repository = get(), constraintsManager = get()) }
        single<CanAddItemToHouseholdUseCase> { CanAddItemToHouseholdUseCaseImpl(constraintsManager = get()) }
        single<CanAddMemberToHouseholdUseCase> { CanAddMemberToHouseholdUseCaseImpl(constraintsManager = get()) }

        // Suggestion Use Cases
        single<ObserveSuggestionsUseCase> { ObserveSuggestionsUseCaseImpl(localDataSource = get()) }
        single<AddOrUpdateSuggestionUseCase> { AddOrUpdateSuggestionUseCaseImpl(localDataSource = get()) }
        single<DeleteSuggestionUseCase> { DeleteSuggestionUseCaseImpl(localDataSource = get()) }
        single<DeleteAllSuggestionsUseCase> { DeleteAllSuggestionsUseCaseImpl(localDataSource = get()) }

        // Permission Use Cases
        // PermissionRepository is bound to a screen's PermissionsController, so these are factories that forward
        // their parameters to it: resolve them with `get { parametersOf(controller) }`.
        factory<CheckPermissionUseCase> { params -> CheckPermissionUseCaseImpl(repository = get { params }) }
        factory<RequestPermissionUseCase> { params -> RequestPermissionUseCaseImpl(repository = get { params }) }
        factory<OpenAppSettingsUseCase> { params -> OpenAppSettingsUseCaseImpl(repository = get { params }) }

        // Loyalty Card Use Cases
        single<ObserveLoyaltyCardsUseCase> { ObserveLoyaltyCardsUseCaseImpl(repository = get()) }
        single<ObserveLoyaltyCardByIdUseCase> { ObserveLoyaltyCardByIdUseCaseImpl(repository = get()) }
        single<GetLoyaltyCardByIdUseCase> { GetLoyaltyCardByIdUseCaseImpl(repository = get()) }
        single<AddLoyaltyCardUseCase> { AddLoyaltyCardUseCaseImpl(repository = get()) }
        single<DeleteLoyaltyCardUseCase> { DeleteLoyaltyCardUseCaseImpl(repository = get()) }
    }
