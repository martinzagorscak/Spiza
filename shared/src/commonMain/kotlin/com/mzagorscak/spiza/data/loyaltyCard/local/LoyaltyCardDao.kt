package com.mzagorscak.spiza.data.loyaltyCard.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mzagorscak.spiza.data.loyaltyCard.model.LoyaltyCardEntity
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import kotlinx.coroutines.flow.Flow

@Dao
interface LoyaltyCardDao {
    @Query("SELECT * FROM loyalty_cards ORDER BY createdAt DESC")
    fun getAll(): Flow<List<LoyaltyCardEntity>>

    @Query("SELECT * FROM loyalty_cards WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<LoyaltyCardEntity?>

    @Query("SELECT * FROM loyalty_cards WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): LoyaltyCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: LoyaltyCardEntity)

    // Not an upsert: REPLACE would reset createdAt (the list order) and isShortcut.
    @Query(
        "UPDATE loyalty_cards SET name = :name, colorArgb = :colorArgb, codeText = :codeText, format = :format WHERE id = :id",
    )
    suspend fun update(
        id: Long,
        name: String,
        colorArgb: Int,
        codeText: String,
        format: LoyaltyCardFormat,
    )

    @Query("DELETE FROM loyalty_cards WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        "UPDATE loyalty_cards SET isShortcut = 1 WHERE id = :id AND " +
            "(isShortcut = 1 OR (SELECT COUNT(*) FROM loyalty_cards WHERE isShortcut = 1) < :limit)",
    )
    suspend fun addShortcutWithinLimit(
        id: Long,
        limit: Int,
    ): Int

    @Query("UPDATE loyalty_cards SET isShortcut = 0 WHERE id = :id")
    suspend fun removeShortcut(id: Long)
}
