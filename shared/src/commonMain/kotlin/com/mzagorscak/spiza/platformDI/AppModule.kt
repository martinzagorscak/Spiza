package com.mzagorscak.spiza.platformDI

import org.koin.dsl.module

val appModule =
    module {
        includes(platformModule())
    }
