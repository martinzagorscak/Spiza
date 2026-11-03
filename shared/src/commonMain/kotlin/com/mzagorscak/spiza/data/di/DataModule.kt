package com.mzagorscak.spiza.data.di

import com.mzagorscak.spiza.data.household.HouseholdRepositoryImpl
import com.mzagorscak.spiza.data.local.SpizaDatabase
import com.mzagorscak.spiza.data.loyaltyCard.LoyaltyCardRepositoryImpl
import com.mzagorscak.spiza.data.loyaltyCard.local.LoyaltyCardDao
import com.mzagorscak.spiza.data.repository.PermissionRepositoryImpl
import com.mzagorscak.spiza.data.settings.SettingsDataSource
import com.mzagorscak.spiza.data.settings.SettingsDataSourceImpl
import com.mzagorscak.spiza.data.settings.SettingsRepositoryImpl
import com.mzagorscak.spiza.data.suggestion.SuggestionRepositoryImpl
import com.mzagorscak.spiza.data.suggestion.local.SuggestionDao
import com.mzagorscak.spiza.data.user.UserRepositoryImpl
import com.mzagorscak.spiza.domain.repository.HouseholdRepository
import com.mzagorscak.spiza.domain.repository.LoyaltyCardRepository
import com.mzagorscak.spiza.domain.repository.PermissionRepository
import com.mzagorscak.spiza.domain.repository.SettingsRepository
import com.mzagorscak.spiza.domain.repository.SuggestionRepository
import com.mzagorscak.spiza.domain.repository.UserRepository
import com.russhwolf.settings.Settings
import dev.icerock.moko.permissions.PermissionsController
import org.koin.dsl.module

val dataModule =
    module {
        single<Settings> { Settings() }
        single<SettingsDataSource> { SettingsDataSourceImpl(settings = get()) }
        single<SettingsRepository> { SettingsRepositoryImpl(dataSource = get()) }
        single<UserRepository> {
            UserRepositoryImpl(
                authService = get(),
                userDataSource = get(),
                crashLogger = get(),
            )
        }
        single<HouseholdRepository> {
            HouseholdRepositoryImpl(
                dataSource = get(),
                authService = get(),
                userRepository = get(),
                constraintsManager = get(),
                crashLogger = get(),
            )
        }
        single<SuggestionDao> { get<SpizaDatabase>().suggestionDao() }
        single<SuggestionRepository> { SuggestionRepositoryImpl(suggestionDao = get()) }
        single<LoyaltyCardDao> { get<SpizaDatabase>().loyaltyCardDao() }
        single<LoyaltyCardRepository> { LoyaltyCardRepositoryImpl(loyaltyCardDao = get()) }
        factory<PermissionRepository> { (permissionsController: PermissionsController) ->
            PermissionRepositoryImpl(permissionsController = permissionsController, crashLogger = get())
        }
    }
