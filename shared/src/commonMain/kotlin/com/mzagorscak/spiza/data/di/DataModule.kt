package com.mzagorscak.spiza.data.di

import com.mzagorscak.spiza.data.household.HouseholdRepositoryImpl
import com.mzagorscak.spiza.data.settings.SettingsDataSource
import com.mzagorscak.spiza.data.settings.SettingsDataSourceImpl
import com.mzagorscak.spiza.data.settings.SettingsRepositoryImpl
import com.mzagorscak.spiza.data.user.UserRepositoryImpl
import com.mzagorscak.spiza.domain.repository.HouseholdRepository
import com.mzagorscak.spiza.domain.repository.SettingsRepository
import com.mzagorscak.spiza.domain.repository.UserRepository
import com.russhwolf.settings.Settings
import org.koin.dsl.module

val dataModule =
    module {
        single<Settings> { Settings() }
        single<SettingsDataSource> { SettingsDataSourceImpl(settings = get()) }
        single<SettingsRepository> { SettingsRepositoryImpl(dataSource = get()) }
        single<UserRepository> { UserRepositoryImpl(authService = get()) }
        single<HouseholdRepository> { HouseholdRepositoryImpl(dataSource = get(), authService = get()) }
    }
