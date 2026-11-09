package com.mzagorscak.spiza.domain.repository

import com.mzagorscak.spiza.domain.model.LoyaltyCard
import kotlinx.coroutines.flow.Flow

interface LoyaltyCardRepository {
    fun observeAll(): Flow<List<LoyaltyCard>>

    fun observeById(id: Long): Flow<LoyaltyCard?>

    suspend fun getById(id: Long): LoyaltyCard?

    suspend fun addCard(card: LoyaltyCard)

    /**
     * Updates the editable fields of the card with [LoyaltyCard.id]; its creation time and shortcut state are kept.
     * Returns `false` when no card has that id (e.g. it was deleted in the meantime).
     */
    suspend fun updateCard(card: LoyaltyCard): Boolean

    suspend fun deleteCard(id: Long)

    suspend fun addShortcut(
        id: Long,
        limit: Int,
    ): Boolean

    suspend fun removeShortcut(id: Long)
}
