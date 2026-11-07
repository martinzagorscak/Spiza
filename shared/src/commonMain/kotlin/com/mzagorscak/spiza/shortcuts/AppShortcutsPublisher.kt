package com.mzagorscak.spiza.shortcuts

import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.LoyaltyCard

/** Platform launcher shortcuts. Bound in `platformModule()`; use cases reach it through `AppShortcutsRepository`. */
interface AppShortcutsPublisher {
    val isSupported: Boolean

    /**
     * [cards] is the full desired set of loyalty card shortcuts, in display order: shortcuts of cards not in it are removed,
     * the rest are pushed or updated. [existingCardIds] are the ids of all stored cards (shortcut or not); a pinned shortcut
     * whose card is not among them was deleted and gets disabled, while one whose card was only un-toggled stays usable.
     * The disabled message (where the platform has one) is resolved in [language], not the system locale. Throws on failure.
     */
    suspend fun publishLoyaltyCards(
        cards: List<LoyaltyCard>,
        existingCardIds: Set<Long>,
        language: AppLanguage,
    )
}
