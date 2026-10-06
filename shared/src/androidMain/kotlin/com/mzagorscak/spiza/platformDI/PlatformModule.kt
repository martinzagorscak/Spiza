package com.mzagorscak.spiza.platformDI

import com.mzagorscak.spiza.crash.AndroidCrashLogger
import com.mzagorscak.spiza.crash.CrashLogger
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module =
    module {
        single<CrashLogger> { AndroidCrashLogger() }
    }
