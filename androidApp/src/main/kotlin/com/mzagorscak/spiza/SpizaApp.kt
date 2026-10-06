package com.mzagorscak.spiza

import android.app.Application
import com.mzagorscak.spiza.platformDI.initKoin
import org.koin.android.ext.koin.androidContext

class SpizaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@SpizaApp)
        }
    }
}
