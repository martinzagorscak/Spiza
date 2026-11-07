package com.mzagorscak.spiza.shortcuts

import com.mzagorscak.spiza.navigation.ShortcutNavigationRequests
import org.koin.mp.KoinPlatform

/**
 * Quick Action contract between [IosAppShortcutsPublisher] and the Swift `SceneDelegate` that receives the taps.
 * The card id is part of the item type instead of `userInfo`, so no Kotlin-bridged dictionary has to survive being
 * persisted by SpringBoard and handed back on tap.
 */
object IosAppShortcuts {
    const val LOYALTY_CARD_TYPE_PREFIX = "com.mzagorscak.spiza.loyaltyCard."

    fun loyaltyCardType(cardId: Long): String = "$LOYALTY_CARD_TYPE_PREFIX$cardId"
}

// Room ids start at 1.
private const val INVALID_CARD_ID = 0L

/**
 * Called from Swift (`IosAppShortcutsKt.handleAppShortcut`) for a tapped Quick Action, so Swift does not touch Koin.
 * Returns whether the action was a loyalty card shortcut; signed out, the navigation drops the request.
 */
fun handleAppShortcut(type: String): Boolean {
    val cardId =
        type
            .removePrefix(IosAppShortcuts.LOYALTY_CARD_TYPE_PREFIX)
            .takeIf { type.startsWith(IosAppShortcuts.LOYALTY_CARD_TYPE_PREFIX) }
            ?.toLongOrNull()
            ?.takeIf { it > INVALID_CARD_ID }
            ?: return false
    KoinPlatform.getKoin().get<ShortcutNavigationRequests>().openLoyaltyCard(cardId)
    return true
}
