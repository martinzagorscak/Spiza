package com.mzagorscak.spiza.platformDI

import com.mzagorscak.spiza.ui.screens.di.viewModelModule
import org.koin.dsl.module

val appModule =
    module {
        includes(
            platformModule(),
            viewModelModule,
        )
    }
