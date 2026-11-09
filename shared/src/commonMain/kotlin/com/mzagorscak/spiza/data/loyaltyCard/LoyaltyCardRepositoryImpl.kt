package com.mzagorscak.spiza.data.loyaltyCard

import com.mzagorscak.spiza.data.loyaltyCard.local.LoyaltyCardDao
import com.mzagorscak.spiza.data.loyaltyCard.model.toEntity
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.repository.LoyaltyCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class LoyaltyCardRepositoryImpl(
    private val loyaltyCardDao: LoyaltyCardDao,
) : LoyaltyCardRepository {
    override fun observeAll(): Flow<List<LoyaltyCard>> =
        loyaltyCardDao.getAll().map { entities ->
            entities.map { it.toDomainModel() }
        }

    override fun observeById(id: Long): Flow<LoyaltyCard?> = loyaltyCardDao.observeById(id).map { it?.toDomainModel() }

    override suspend fun getById(id: Long): LoyaltyCard? = loyaltyCardDao.getById(id)?.toDomainModel()

    override suspend fun addCard(card: LoyaltyCard) {
        loyaltyCardDao.insert(card.toEntity())
    }

    override suspend fun updateCard(card: LoyaltyCard) {
        loyaltyCardDao.update(
            id = card.id,
            name = card.name,
            colorArgb = card.colorArgb,
            codeText = card.codeText,
            format = card.format,
        )
    }

    override suspend fun deleteCard(id: Long) {
        loyaltyCardDao.deleteById(id)
    }

    override suspend fun addShortcut(
        id: Long,
        limit: Int,
    ): Boolean = loyaltyCardDao.addShortcutWithinLimit(id = id, limit = limit) > 0

    override suspend fun removeShortcut(id: Long) {
        loyaltyCardDao.removeShortcut(id)
    }
}
