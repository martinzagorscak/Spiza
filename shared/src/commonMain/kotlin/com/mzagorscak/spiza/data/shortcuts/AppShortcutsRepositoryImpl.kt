package com.mzagorscak.spiza.data.shortcuts

import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.catchAndRecord
import com.mzagorscak.spiza.crash.recordError
import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.domain.repository.AppShortcutsRepository
import com.mzagorscak.spiza.shortcuts.AppShortcutsPublisher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

internal class AppShortcutsRepositoryImpl(
    private val publisher: AppShortcutsPublisher,
    private val crashLogger: CrashLogger,
) : AppShortcutsRepository {
    override val isSupported: Boolean
        get() = publisher.isSupported

    override suspend fun syncLoyaltyCardShortcuts(
        allCards: Flow<List<LoyaltyCard>>,
        language: Flow<AppLanguage>,
    ) {
        if (publisher.isSupported.not()) return
        combine(allCards, language) { cards, currentLanguage ->
            LoyaltyCardShortcuts(
                cards = cards.filter { it.isShortcut },
                existingCardIds = cards.mapTo(mutableSetOf()) { it.id },
                language = currentLanguage,
            )
        }.distinctUntilChanged()
            .catchAndRecord(crashLogger = crashLogger, context = "Loyalty card shortcuts stream failed")
            .collectLatest { shortcuts ->
                try {
                    publisher.publishLoyaltyCards(
                        cards = shortcuts.cards,
                        existingCardIds = shortcuts.existingCardIds,
                        language = shortcuts.language,
                    )
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    crashLogger.recordError(context = "Publishing loyalty card shortcuts failed", error = error)
                }
            }
    }

    private data class LoyaltyCardShortcuts(
        val cards: List<LoyaltyCard>,
        val existingCardIds: Set<Long>,
        val language: AppLanguage,
    )
}
