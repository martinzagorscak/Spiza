package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import com.mzagorscak.spiza.shortcuts.AppShortcutsPublisher

/** Records every publish attempt **before** [publishFailure] is thrown. */
class FakeAppShortcutsPublisher(
    override val isSupported: Boolean = true,
) : AppShortcutsPublisher {
    data class Published(
        val cardIds: List<Long>,
        val existingCardIds: Set<Long>,
        val language: AppLanguage,
    )

    /** When set, [publishLoyaltyCards] throws it (like a launcher that rejects the shortcuts). */
    var publishFailure: Exception? = null

    val published = mutableListOf<Published>()

    override suspend fun publishLoyaltyCards(
        cards: List<LoyaltyCard>,
        existingCardIds: Set<Long>,
        language: AppLanguage,
    ) {
        published += Published(cardIds = cards.map { it.id }, existingCardIds = existingCardIds, language = language)
        publishFailure?.let { throw it }
    }
}
