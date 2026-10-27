package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.repository.LoyaltyCardRepository
import kotlinx.coroutines.flow.Flow

interface ObserveLoyaltyCardsUseCase {
    operator fun invoke(): Flow<List<LoyaltyCard>>
}

class ObserveLoyaltyCardsUseCaseImpl(
    private val repository: LoyaltyCardRepository,
) : ObserveLoyaltyCardsUseCase {
    override operator fun invoke(): Flow<List<LoyaltyCard>> = repository.observeAll()
}

interface ObserveLoyaltyCardByIdUseCase {
    operator fun invoke(id: Long): Flow<LoyaltyCard?>
}

class ObserveLoyaltyCardByIdUseCaseImpl(
    private val repository: LoyaltyCardRepository,
) : ObserveLoyaltyCardByIdUseCase {
    override operator fun invoke(id: Long): Flow<LoyaltyCard?> = repository.observeById(id)
}

interface GetLoyaltyCardByIdUseCase {
    suspend operator fun invoke(id: Long): LoyaltyCard?
}

class GetLoyaltyCardByIdUseCaseImpl(
    private val repository: LoyaltyCardRepository,
) : GetLoyaltyCardByIdUseCase {
    override suspend operator fun invoke(id: Long): LoyaltyCard? = repository.getById(id)
}

interface AddLoyaltyCardUseCase {
    suspend operator fun invoke(card: LoyaltyCard)
}

class AddLoyaltyCardUseCaseImpl(
    private val repository: LoyaltyCardRepository,
) : AddLoyaltyCardUseCase {
    override suspend operator fun invoke(card: LoyaltyCard) = repository.addCard(card)
}

interface DeleteLoyaltyCardUseCase {
    suspend operator fun invoke(id: Long)
}

class DeleteLoyaltyCardUseCaseImpl(
    private val repository: LoyaltyCardRepository,
) : DeleteLoyaltyCardUseCase {
    override suspend operator fun invoke(id: Long) = repository.deleteCard(id)
}
