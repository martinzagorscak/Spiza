package com.mzagorscak.spiza.platformDI

import com.mzagorscak.spiza.auth.AndroidAuthService
import com.mzagorscak.spiza.auth.AuthService
import com.mzagorscak.spiza.crash.AndroidCrashLogger
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.data.household.FirestoreHouseholdDataSource
import com.mzagorscak.spiza.data.household.HouseholdDataSource
import com.mzagorscak.spiza.data.local.SpizaDatabase
import com.mzagorscak.spiza.data.local.getAndroidSpizaDatabaseBuilder
import com.mzagorscak.spiza.data.local.getSpizaDatabase
import com.mzagorscak.spiza.data.user.FirestoreUserDataSource
import com.mzagorscak.spiza.data.user.UserDataSource
import com.mzagorscak.spiza.shortcuts.AndroidAppShortcutsPublisher
import com.mzagorscak.spiza.shortcuts.AppShortcutsPublisher
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

const val WEB_CLIENT_ID = "webClientId"

actual fun platformModule(): Module =
    module {
        single<CrashLogger> { AndroidCrashLogger() }
        single<UserDataSource> { FirestoreUserDataSource() }
        single<HouseholdDataSource> { FirestoreHouseholdDataSource(crashLogger = get()) }
        single<AuthService> {
            AndroidAuthService(
                context = androidContext(),
                activityProvider = get(),
                webClientId = get(named(WEB_CLIENT_ID)),
                crashLogger = get(),
            )
        }
        single<SpizaDatabase> {
            val builder = getAndroidSpizaDatabaseBuilder(androidContext())
            getSpizaDatabase(builder)
        }
        single<AppShortcutsPublisher> { AndroidAppShortcutsPublisher(context = androidContext()) }
    }
