package com.mzagorscak.spiza.navigation

/** The root destination: sign-in first, then the one-time onboarding, then the shopping list. */
internal fun startRoute(
    isSignedIn: Boolean,
    isOnboardingCompleted: Boolean,
): Route =
    when {
        isSignedIn.not() -> Route.Login
        isOnboardingCompleted.not() -> Route.Onboarding()
        else -> Route.ShoppingList
    }

/**
 * The back stack that opens the loyalty card [cardId] from an app shortcut. The card opens on top of [startRoute] so the root
 * still matches it: a shortcut used at checkout is not blocked by a pending onboarding, which is shown again on back.
 */
internal fun loyaltyCardShortcutBackStack(
    startRoute: Route,
    cardId: Long,
): List<Route> =
    if (startRoute == Route.ShoppingList) {
        listOf(Route.ShoppingList, Route.LoyaltyCards, Route.LoyaltyCardDetails(cardId))
    } else {
        listOf(startRoute, Route.LoyaltyCardDetails(cardId))
    }
