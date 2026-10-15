package com.mzagorscak.spiza.data.suggestion.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "item_suggestions",
    indices = [Index(value = ["normalizedName"], unique = true)],
)
data class SuggestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val normalizedName: String,
    val useCount: Int = 1,
    val lastUsedAt: Long,
)
