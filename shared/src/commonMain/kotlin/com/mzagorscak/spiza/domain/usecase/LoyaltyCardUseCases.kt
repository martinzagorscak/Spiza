package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.domain.loyaltyCard.LoyaltyCardCodes
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.model.LoyaltyCardCode
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
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

interface ValidateLoyaltyCardCodeUseCase {
    operator fun invoke(code: LoyaltyCardCode): Boolean
}

class ValidateLoyaltyCardCodeUseCaseImpl : ValidateLoyaltyCardCodeUseCase {
    override operator fun invoke(code: LoyaltyCardCode): Boolean = LoyaltyCardCodes.isValid(code)
}

/** Whether typed or pasted input fits the longest code any format accepts. */
interface CanEnterLoyaltyCardCodeUseCase {
    operator fun invoke(value: String): Boolean
}

class CanEnterLoyaltyCardCodeUseCaseImpl : CanEnterLoyaltyCardCodeUseCase {
    override operator fun invoke(value: String): Boolean = LoyaltyCardCodes.canEnter(value)
}

interface DetectLoyaltyCardFormatUseCase {
    operator fun invoke(value: String): LoyaltyCardFormat
}

class DetectLoyaltyCardFormatUseCaseImpl : DetectLoyaltyCardFormatUseCase {
    override operator fun invoke(value: String): LoyaltyCardFormat = LoyaltyCardCodes.detectFormat(value)
}

interface NormalizeScannedLoyaltyCardCodeUseCase {
    operator fun invoke(code: LoyaltyCardCode): LoyaltyCardCode
}

class NormalizeScannedLoyaltyCardCodeUseCaseImpl : NormalizeScannedLoyaltyCardCodeUseCase {
    override operator fun invoke(code: LoyaltyCardCode): LoyaltyCardCode = LoyaltyCardCodes.normalizeScanned(code)
}
