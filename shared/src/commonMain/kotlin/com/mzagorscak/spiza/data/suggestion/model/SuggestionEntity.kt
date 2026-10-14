package com.mzagorscak.spiza.data.suggestion.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "item_suggestions",
    indices = [Index(value = ["normalized_name"], unique = true)],
)
data class SuggestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,
    @ColumnInfo(name = "use_count")
    val useCount: Int = 1,
    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long,
)
