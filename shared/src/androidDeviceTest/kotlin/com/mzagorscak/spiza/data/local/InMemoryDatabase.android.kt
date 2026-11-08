package com.mzagorscak.spiza.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual fun inMemorySpizaDatabase(): SpizaDatabase =
    Room
        .inMemoryDatabaseBuilder<SpizaDatabase>(context = ApplicationProvider.getApplicationContext<Context>())
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
