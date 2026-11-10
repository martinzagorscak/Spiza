package com.mzagorscak.spiza.platformDI

import com.mzagorscak.spiza.auth.IosAuthBridge
import com.mzagorscak.spiza.crash.IosCrashLoggerBridge
import com.mzagorscak.spiza.data.account.IosAccountBridge
import com.mzagorscak.spiza.data.household.IosHouseholdBridge
import com.mzagorscak.spiza.data.remoteConfig.IosRemoteConfigBridge
import com.mzagorscak.spiza.data.user.IosUserBridge
import org.koin.dsl.module

fun initKoinIos(
    crashLoggerBridge: IosCrashLoggerBridge,
    authBridge: IosAuthBridge,
    userBridge: IosUserBridge,
    householdBridge: IosHouseholdBridge,
    accountBridge: IosAccountBridge,
    remoteConfigBridge: IosRemoteConfigBridge,
) {
    initKoin {
        modules(
            module {
                single<IosCrashLoggerBridge> { crashLoggerBridge }
                single<IosAuthBridge> { authBridge }
                single<IosUserBridge> { userBridge }
                single<IosHouseholdBridge> { householdBridge }
                single<IosAccountBridge> { accountBridge }
                single<IosRemoteConfigBridge> { remoteConfigBridge }
            },
        )
    }
}
