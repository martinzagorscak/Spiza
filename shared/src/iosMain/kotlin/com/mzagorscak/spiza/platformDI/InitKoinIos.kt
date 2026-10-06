package com.mzagorscak.spiza.platformDI

import com.mzagorscak.spiza.crash.IosCrashLoggerBridge
import org.koin.dsl.module

fun initKoinIos(crashLoggerBridge: IosCrashLoggerBridge) {
    initKoin {
        modules(
            module {
                single<IosCrashLoggerBridge> { crashLoggerBridge }
            },
        )
    }
}
