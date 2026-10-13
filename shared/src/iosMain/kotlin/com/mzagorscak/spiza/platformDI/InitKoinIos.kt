package com.mzagorscak.spiza.platformDI

import com.mzagorscak.spiza.auth.IosAuthBridge
import com.mzagorscak.spiza.crash.IosCrashLoggerBridge
import com.mzagorscak.spiza.data.household.IosHouseholdBridge
import org.koin.dsl.module

fun initKoinIos(
    crashLoggerBridge: IosCrashLoggerBridge,
    authBridge: IosAuthBridge,
    householdBridge: IosHouseholdBridge,
) {
    initKoin {
        modules(
            module {
                single<IosCrashLoggerBridge> { crashLoggerBridge }
                single<IosAuthBridge> { authBridge }
                single<IosHouseholdBridge> { householdBridge }
            },
        )
    }
}
