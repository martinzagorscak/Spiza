package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.domain.constraints.ConstraintsManager
import com.mzagorscak.spiza.domain.loyaltyCard.LoyaltyCardCodes
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.model.LoyaltyCardCode
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.domain.model.ShortcutToggleResult
import com.mzagorscak.spiza.domain.repository.AppShortcutsRepository
import com.mzagorscak.spiza.domain.repository.LoyaltyCardRepository
import com.mzagorscak.spiza.domain.repository.SettingsRepository
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

/** Pins the card as an app shortcut if it is not one yet (within the limit), otherwise unpins it. */
interface ToggleLoyaltyCardShortcutUseCase {
    suspend operator fun invoke(id: Long): ShortcutToggleResult
}

class ToggleLoyaltyCardShortcutUseCaseImpl(
    private val repository: LoyaltyCardRepository,
    private val constraintsManager: ConstraintsManager,
) : ToggleLoyaltyCardShortcutUseCase {
    override suspend operator fun invoke(id: Long): ShortcutToggleResult {
        val card = repository.getById(id) ?: return ShortcutToggleResult.CardNotFound
        if (card.isShortcut) {
            repository.removeShortcut(id)
            return ShortcutToggleResult.Disabled
        }
        val limit = constraintsManager.maxLoyaltyCardShortcuts
        return when {
            repository.addShortcut(id = id, limit = limit) -> ShortcutToggleResult.Enabled
            repository.getById(id) == null -> ShortcutToggleResult.CardNotFound
            else -> ShortcutToggleResult.LimitReached(limit = limit)
        }
    }
}

interface CanUseAppShortcutsUseCase {
    operator fun invoke(): Boolean
}

class CanUseAppShortcutsUseCaseImpl(
    private val repository: AppShortcutsRepository,
) : CanUseAppShortcutsUseCase {
    override operator fun invoke(): Boolean = repository.isSupported
}

interface SyncLoyaltyCardShortcutsUseCase {
    suspend operator fun invoke()
}

class SyncLoyaltyCardShortcutsUseCaseImpl(
    private val loyaltyCardRepository: LoyaltyCardRepository,
    private val settingsRepository: SettingsRepository,
    private val appShortcutsRepository: AppShortcutsRepository,
) : SyncLoyaltyCardShortcutsUseCase {
    override suspend operator fun invoke() {
        appShortcutsRepository.syncLoyaltyCardShortcuts(
            allCards = loyaltyCardRepository.observeAll(),
            language = settingsRepository.language,
        )
    }
}
