package com.mzagorscak.spiza.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ShortcutNavigationRequests {
    private val pendingLoyaltyCardId = MutableStateFlow<Long?>(null)

    val loyaltyCardId: StateFlow<Long?> = pendingLoyaltyCardId.asStateFlow()

    fun openLoyaltyCard(cardId: Long) {
        pendingLoyaltyCardId.value = cardId
    }

    fun consume(cardId: Long) {
        pendingLoyaltyCardId.compareAndSet(expect = cardId, update = null)
    }
}
