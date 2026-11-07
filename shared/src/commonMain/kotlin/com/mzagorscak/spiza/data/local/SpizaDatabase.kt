package com.mzagorscak.spiza.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.mzagorscak.spiza.data.loyaltyCard.local.LoyaltyCardDao
import com.mzagorscak.spiza.data.loyaltyCard.model.LoyaltyCardEntity
import com.mzagorscak.spiza.data.suggestion.local.SuggestionDao
import com.mzagorscak.spiza.data.suggestion.model.SuggestionEntity

@Database(
    entities = [
        SuggestionEntity::class,
        LoyaltyCardEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@ConstructedBy(SpizaDatabaseConstructor::class)
abstract class SpizaDatabase : RoomDatabase() {
    abstract fun suggestionDao(): SuggestionDao

    abstract fun loyaltyCardDao(): LoyaltyCardDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SpizaDatabaseConstructor : RoomDatabaseConstructor<SpizaDatabase> {
    override fun initialize(): SpizaDatabase
}
