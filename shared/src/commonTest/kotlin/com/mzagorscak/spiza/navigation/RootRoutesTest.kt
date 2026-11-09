package com.mzagorscak.spiza.navigation

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlin.test.Test

class RootRoutesTest {
    @Test
    fun startRoute_isLogin_whenSignedOut() {
        assertThat(startRoute(isSignedIn = false, isOnboardingCompleted = false)).isEqualTo(Route.Login)
    }

    @Test
    fun startRoute_isOnboarding_whenSignedInAndOnboardingNotCompleted() {
        assertThat(startRoute(isSignedIn = true, isOnboardingCompleted = false)).isEqualTo(Route.Onboarding(isReplay = false))
    }

    @Test
    fun startRoute_isShoppingList_whenSignedInAndOnboardingCompleted() {
        assertThat(startRoute(isSignedIn = true, isOnboardingCompleted = true)).isEqualTo(Route.ShoppingList)
    }

    @Test
    fun loyaltyCardShortcutBackStack_opensTheCardFromTheLoyaltyCardsTab() {
        val backStack = loyaltyCardShortcutBackStack(startRoute = Route.ShoppingList, cardId = 7L)

        assertThat(backStack).containsExactly(Route.ShoppingList, Route.LoyaltyCards, Route.LoyaltyCardDetails(7L))
    }

    @Test
    fun loyaltyCardShortcutBackStack_keepsThePendingOnboardingAsRoot() {
        // The root reset keeps a stack whose root matches the start route, so the card stays open (e.g. on rotation).
        val backStack = loyaltyCardShortcutBackStack(startRoute = Route.Onboarding(), cardId = 7L)

        assertThat(backStack).containsExactly(Route.Onboarding(), Route.LoyaltyCardDetails(7L))
    }
}
