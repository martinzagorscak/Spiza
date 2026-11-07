package com.mzagorscak.spiza.shortcuts

import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationShortcutIcon
import platform.UIKit.UIApplicationShortcutItem
import platform.UIKit.shortcutItems

// Monochrome SF Symbol: Quick Action icons are templates tinted by the system, so the card colour cannot be shown.
private const val LOYALTY_CARD_ICON_SYSTEM_NAME = "creditcard"

/**
 * Home Screen Quick Actions. Dynamic items are replaced as a whole and iOS has no pinned shortcuts, so a deleted card's
 * action simply disappears and [existingCardIds] is not needed.
 */
internal class IosAppShortcutsPublisher : AppShortcutsPublisher {
    override val isSupported: Boolean = true

    override suspend fun publishLoyaltyCards(
        cards: List<LoyaltyCard>,
        existingCardIds: Set<Long>,
        language: AppLanguage,
    ) {
        // [language] is unused: titles are the card names and iOS has no disabled-shortcut message to localize.
        // UIApplication may only be touched on the main thread.
        withContext(Dispatchers.Main) {
            val application = UIApplication.sharedApplication
            val otherItems =
                application.shortcutItems
                    .orEmpty()
                    .filterIsInstance<UIApplicationShortcutItem>()
                    .filterNot { it.type.startsWith(IosAppShortcuts.LOYALTY_CARD_TYPE_PREFIX) }
            val loyaltyCardItems =
                cards.map { card ->
                    UIApplicationShortcutItem(
                        type = IosAppShortcuts.loyaltyCardType(cardId = card.id),
                        localizedTitle = card.name.trim(),
                        localizedSubtitle = null,
                        icon = UIApplicationShortcutIcon.iconWithSystemImageName(LOYALTY_CARD_ICON_SYSTEM_NAME),
                        userInfo = null,
                    )
                }
            application.shortcutItems = loyaltyCardItems + otherItems
        }
    }
}
