package com.mzagorscak.spiza.platformDI

import com.mzagorscak.spiza.auth.IosAuthBridge
import com.mzagorscak.spiza.crash.IosCrashLoggerBridge
import org.koin.dsl.module

fun initKoinIos(
    crashLoggerBridge: IosCrashLoggerBridge,
    authBridge: IosAuthBridge,
) {
    initKoin {
        modules(
            module {
                single<IosCrashLoggerBridge> { crashLoggerBridge }
                single<IosAuthBridge> { authBridge }
            },
        )
    }
}
