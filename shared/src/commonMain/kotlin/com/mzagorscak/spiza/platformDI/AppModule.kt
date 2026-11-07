package com.mzagorscak.spiza.platformDI

import com.mzagorscak.spiza.data.di.dataModule
import com.mzagorscak.spiza.domain.di.domainModule
import com.mzagorscak.spiza.navigation.di.navigationModule
import com.mzagorscak.spiza.ui.screens.di.viewModelModule
import org.koin.dsl.module

val appModule =
    module {
        includes(
            platformModule(),
            dataModule,
            domainModule,
            viewModelModule,
            navigationModule,
        )
    }
