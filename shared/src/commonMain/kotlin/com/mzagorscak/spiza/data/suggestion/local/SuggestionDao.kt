package com.mzagorscak.spiza.data.suggestion.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mzagorscak.spiza.data.suggestion.model.SuggestionEntity
import com.mzagorscak.spiza.data.suggestion.model.SuggestionSortingType
import kotlinx.coroutines.flow.Flow

@Dao
interface SuggestionDao {
    // read

    @Query("SELECT * FROM item_suggestions ORDER BY lastUsedAt DESC")
    fun getAllLastUsedDesc(): Flow<List<SuggestionEntity>>

    @Query("SELECT * FROM item_suggestions ORDER BY lastUsedAt ASC")
    fun getAllLastUsedAsc(): Flow<List<SuggestionEntity>>

    @Query("SELECT * FROM item_suggestions ORDER BY name COLLATE NOCASE ASC")
    fun getAllAlphabeticalAsc(): Flow<List<SuggestionEntity>>

    @Query("SELECT * FROM item_suggestions ORDER BY name COLLATE NOCASE DESC")
    fun getAllAlphabeticalDesc(): Flow<List<SuggestionEntity>>

    @Query("SELECT * FROM item_suggestions ORDER BY useCount DESC, lastUsedAt DESC")
    fun getAllMostUsedDesc(): Flow<List<SuggestionEntity>>

    @Query("SELECT * FROM item_suggestions ORDER BY useCount ASC, lastUsedAt ASC")
    fun getAllMostUsedAsc(): Flow<List<SuggestionEntity>>

    fun getAll(sortingType: SuggestionSortingType): Flow<List<SuggestionEntity>> =
        when (sortingType) {
            SuggestionSortingType.LAST_USED_DESC -> getAllLastUsedDesc()
            SuggestionSortingType.LAST_USED_ASC -> getAllLastUsedAsc()
            SuggestionSortingType.ALPHABETICAL_ASC -> getAllAlphabeticalAsc()
            SuggestionSortingType.ALPHABETICAL_DESC -> getAllAlphabeticalDesc()
            SuggestionSortingType.MOST_USED_DESC -> getAllMostUsedDesc()
            SuggestionSortingType.MOST_USED_ASC -> getAllMostUsedAsc()
        }

    @Query("SELECT * FROM item_suggestions WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun getByNormalizedName(normalizedName: String): SuggestionEntity?

    // write

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(suggestion: SuggestionEntity)

    @Transaction
    suspend fun upsert(
        name: String,
        normalizedName: String,
        lastUsedAt: Long,
    ) {
        val existing = getByNormalizedName(normalizedName)
        if (existing != null) {
            insert(
                existing.copy(
                    name = name,
                    useCount = existing.useCount + 1,
                    lastUsedAt = lastUsedAt,
                ),
            )
        } else {
            insert(
                SuggestionEntity(
                    name = name,
                    normalizedName = normalizedName,
                    useCount = 1,
                    lastUsedAt = lastUsedAt,
                ),
            )
        }
    }

    // delete

    @Query("DELETE FROM item_suggestions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
