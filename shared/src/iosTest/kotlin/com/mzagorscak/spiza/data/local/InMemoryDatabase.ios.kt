package com.mzagorscak.spiza.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual fun inMemorySpizaDatabase(): SpizaDatabase =
    Room
        .inMemoryDatabaseBuilder<SpizaDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
