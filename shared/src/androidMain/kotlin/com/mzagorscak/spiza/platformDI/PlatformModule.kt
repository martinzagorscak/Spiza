package com.mzagorscak.spiza.platformDI

import com.mzagorscak.spiza.auth.AndroidAuthService
import com.mzagorscak.spiza.auth.AuthService
import com.mzagorscak.spiza.crash.AndroidCrashLogger
import com.mzagorscak.spiza.crash.CrashLogger
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

const val WEB_CLIENT_ID = "webClientId"

actual fun platformModule(): Module =
    module {
        single<CrashLogger> { AndroidCrashLogger() }
        single<AuthService> {
            AndroidAuthService(
                context = androidContext(),
                activityProvider = get(),
                webClientId = get(named(WEB_CLIENT_ID)),
            )
        }
    }
