package com.mzagorscak.spiza.data.suggestion.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getAndroidSpizaDatabaseBuilder(context: Context): RoomDatabase.Builder<SpizaDatabase> {
    val dbFile = context.getDatabasePath("spiza_database.db")
    return Room.databaseBuilder<SpizaDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath,
    )
}
