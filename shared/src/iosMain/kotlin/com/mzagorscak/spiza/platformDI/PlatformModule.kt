package com.mzagorscak.spiza.platformDI

import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.IosCrashLogger
import com.mzagorscak.spiza.crash.IosCrashLoggerBridge
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module =
    module {
        single<CrashLogger> { IosCrashLogger(bridge = getOrNull<IosCrashLoggerBridge>()) }
    }
