package com.mzagorscak.spiza.platformDI

import com.mzagorscak.spiza.auth.AuthService
import com.mzagorscak.spiza.auth.IosAuthService
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.IosCrashLogger
import com.mzagorscak.spiza.crash.IosCrashLoggerBridge
import com.mzagorscak.spiza.data.appUpdate.AppVersionProvider
import com.mzagorscak.spiza.data.appUpdate.IosAppVersionProvider
import com.mzagorscak.spiza.data.household.HouseholdDataSource
import com.mzagorscak.spiza.data.household.IosHouseholdDataSource
import com.mzagorscak.spiza.data.local.SpizaDatabase
import com.mzagorscak.spiza.data.local.getIosSpizaDatabaseBuilder
import com.mzagorscak.spiza.data.local.getSpizaDatabase
import com.mzagorscak.spiza.data.remoteConfig.IosRemoteConfigDataSource
import com.mzagorscak.spiza.data.remoteConfig.RemoteConfigDataSource
import com.mzagorscak.spiza.data.user.IosUserDataSource
import com.mzagorscak.spiza.data.user.UserDataSource
import com.mzagorscak.spiza.shortcuts.AppShortcutsPublisher
import com.mzagorscak.spiza.shortcuts.IosAppShortcutsPublisher
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module =
    module {
        single<CrashLogger> { IosCrashLogger(bridge = getOrNull<IosCrashLoggerBridge>()) }
        single<AuthService> {
            IosAuthService(
                bridge = get(),
                crashLogger = get(),
            )
        }
        single<UserDataSource> { IosUserDataSource(bridge = get()) }
        single<HouseholdDataSource> { IosHouseholdDataSource(bridge = get()) }
        single<RemoteConfigDataSource> { IosRemoteConfigDataSource(bridge = get()) }
        single<AppVersionProvider> { IosAppVersionProvider() }
        single<SpizaDatabase> {
            val builder = getIosSpizaDatabaseBuilder()
            getSpizaDatabase(builder)
        }
        single<AppShortcutsPublisher> { IosAppShortcutsPublisher() }
    }
