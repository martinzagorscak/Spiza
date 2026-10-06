package com.mzagorscak.spiza.platformDI

import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes

fun initKoin(configuration: KoinAppDeclaration? = null) {
    startKoin {
        includes(configuration)
        modules(appModule)
        printLogger(Level.DEBUG)
    }
}
