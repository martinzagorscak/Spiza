package com.mzagorscak.spiza.platformDI

import com.mzagorscak.spiza.auth.AuthService
import com.mzagorscak.spiza.auth.IosAuthService
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.IosCrashLogger
import com.mzagorscak.spiza.crash.IosCrashLoggerBridge
import com.mzagorscak.spiza.data.household.HouseholdDataSource
import com.mzagorscak.spiza.data.household.IosHouseholdDataSource
import com.mzagorscak.spiza.data.suggestion.local.SpizaDatabase
import com.mzagorscak.spiza.data.suggestion.local.getIosSpizaDatabaseBuilder
import com.mzagorscak.spiza.data.suggestion.local.getSpizaDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module =
    module {
        single<CrashLogger> { IosCrashLogger(bridge = getOrNull<IosCrashLoggerBridge>()) }
        single<AuthService> { IosAuthService(bridge = get()) }
        single<HouseholdDataSource> { IosHouseholdDataSource(bridge = get()) }
        single<SpizaDatabase> {
            val builder = getIosSpizaDatabaseBuilder()
            getSpizaDatabase(builder)
        }
    }
