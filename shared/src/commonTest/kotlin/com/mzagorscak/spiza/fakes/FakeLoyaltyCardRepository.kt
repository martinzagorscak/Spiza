package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.repository.LoyaltyCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeLoyaltyCardRepository : LoyaltyCardRepository {
    val cards = MutableStateFlow<List<LoyaltyCard>>(emptyList())
    val addedCards = mutableListOf<LoyaltyCard>()

    override fun observeAll(): Flow<List<LoyaltyCard>> = cards

    override fun observeById(id: Long): Flow<LoyaltyCard?> = cards.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getById(id: Long): LoyaltyCard? = cards.value.firstOrNull { it.id == id }

    override suspend fun addCard(card: LoyaltyCard) {
        addedCards += card
        cards.value += card
    }

    override suspend fun deleteCard(id: Long) {
        cards.value = cards.value.filterNot { it.id == id }
    }
}
