package com.mzagorscak.spiza

import android.app.Application
import com.mzagorscak.spiza.auth.CurrentActivityProvider
import com.mzagorscak.spiza.auth.CurrentActivityProviderImpl
import com.mzagorscak.spiza.platformDI.WEB_CLIENT_ID
import com.mzagorscak.spiza.platformDI.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

class SpizaApp : Application() {

    private val activityProvider = CurrentActivityProviderImpl()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(activityProvider)
        initKoin(enableLogger = BuildConfig.IS_DEBUGGABLE) {
            androidContext(this@SpizaApp)
            modules(
                module {
                    single<CurrentActivityProvider> { activityProvider }
                    single(named(WEB_CLIENT_ID)) { getString(R.string.default_web_client_id) }
                },
            )
        }
    }
}
