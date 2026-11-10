package com.mzagorscak.spiza.platformDI

import android.content.pm.ApplicationInfo
import com.mzagorscak.spiza.auth.AndroidAuthService
import com.mzagorscak.spiza.auth.AuthService
import com.mzagorscak.spiza.crash.AndroidCrashLogger
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.data.account.AccountDataSource
import com.mzagorscak.spiza.data.account.FirestoreAccountDataSource
import com.mzagorscak.spiza.data.appUpdate.AndroidAppVersionProvider
import com.mzagorscak.spiza.data.appUpdate.AppVersionProvider
import com.mzagorscak.spiza.data.household.FirestoreHouseholdDataSource
import com.mzagorscak.spiza.data.household.HouseholdDataSource
import com.mzagorscak.spiza.data.local.SpizaDatabase
import com.mzagorscak.spiza.data.local.getAndroidSpizaDatabaseBuilder
import com.mzagorscak.spiza.data.local.getSpizaDatabase
import com.mzagorscak.spiza.data.remoteConfig.FirebaseRemoteConfigDataSource
import com.mzagorscak.spiza.data.remoteConfig.RemoteConfigDataSource
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
        single<AccountDataSource> { FirestoreAccountDataSource() }
        single<RemoteConfigDataSource> {
            FirebaseRemoteConfigDataSource(
                isDebuggable = (androidContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
            )
        }
        single<AppVersionProvider> { AndroidAppVersionProvider(context = androidContext()) }
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
