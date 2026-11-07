package com.mzagorscak.spiza.navigation.di

import com.mzagorscak.spiza.navigation.ShortcutNavigationRequests
import org.koin.dsl.module

val navigationModule =
    module {
        single<ShortcutNavigationRequests> { ShortcutNavigationRequests() }
    }
