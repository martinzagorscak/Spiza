package com.mzagorscak.spiza.navigation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ShortcutNavigationRequestsTest {
    private val requests = ShortcutNavigationRequests()

    @Test
    fun openLoyaltyCard_makesTheCardPending_untilItIsConsumed() =
        runTest {
            requests.loyaltyCardId.test {
                assertThat(awaitItem()).isNull()

                requests.openLoyaltyCard(cardId = 7L)
                assertThat(awaitItem()).isEqualTo(7L)

                requests.consume(cardId = 7L)
                assertThat(awaitItem()).isNull()
            }
        }

    @Test
    fun openLoyaltyCard_replacesAnEarlierPendingCard() {
        requests.openLoyaltyCard(cardId = 7L)

        requests.openLoyaltyCard(cardId = 8L)

        assertThat(requests.loyaltyCardId.value).isEqualTo(8L)
    }

    // region Error handling

    @Test
    fun consume_keepsANewerRequest_whenAnOlderCardIsConsumed() {
        requests.openLoyaltyCard(cardId = 7L)
        requests.openLoyaltyCard(cardId = 8L)

        requests.consume(cardId = 7L)

        assertThat(requests.loyaltyCardId.value).isEqualTo(8L)
    }

    // endregion
}
