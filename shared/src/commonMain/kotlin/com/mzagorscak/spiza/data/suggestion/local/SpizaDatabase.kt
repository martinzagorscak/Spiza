package com.mzagorscak.spiza.data.suggestion.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.mzagorscak.spiza.data.suggestion.model.SuggestionEntity

@Database(entities = [SuggestionEntity::class], version = 1, exportSchema = false)
@ConstructedBy(SpizaDatabaseConstructor::class)
abstract class SpizaDatabase : RoomDatabase() {
    abstract fun suggestionDao(): SuggestionDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SpizaDatabaseConstructor : RoomDatabaseConstructor<SpizaDatabase> {
    override fun initialize(): SpizaDatabase
}
