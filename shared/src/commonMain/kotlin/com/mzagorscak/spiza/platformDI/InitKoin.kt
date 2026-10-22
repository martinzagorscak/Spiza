package com.mzagorscak.spiza.platformDI

import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes

fun initKoin(
    enableLogger: Boolean = true,
    configuration: KoinAppDeclaration? = null,
) {
    startKoin {
        includes(configuration)
        modules(appModule)
        if (enableLogger) printLogger(Level.INFO) else printLogger(Level.NONE)
    }
}
