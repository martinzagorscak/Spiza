package com.mzagorscak.spiza.data.loyaltyCard.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mzagorscak.spiza.data.loyaltyCard.model.LoyaltyCardEntity
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

    @Query("DELETE FROM loyalty_cards WHERE id = :id")
    suspend fun deleteById(id: Long)
}
