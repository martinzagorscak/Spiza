package com.mzagorscak.spiza.domain.repository

import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import kotlinx.coroutines.flow.Flow

interface AppShortcutsRepository {
    val isSupported: Boolean

    /**
     * Publishes the cards of [allCards] that are shortcuts until cancelled. [allCards] must contain every stored card
     * (not only shortcuts) so a deleted card can be told apart from one whose shortcut was only switched off.
     * The disabled-shortcut message is in the in-app [language] and is re-published when it changes.
     */
    suspend fun syncLoyaltyCardShortcuts(
        allCards: Flow<List<LoyaltyCard>>,
        language: Flow<AppLanguage>,
    )
}
