package com.mzagorscak.spiza.ui.screens.loyaltyCards

import androidx.lifecycle.ViewModel
import com.mzagorscak.spiza.ui.model.PresentableLoyaltyCard
import kotlinx.coroutines.flow.StateFlow

data class LoyaltyCardsState(
    val cards: List<PresentableLoyaltyCard> = emptyList(),
    val isLoading: Boolean = false,
)

abstract class LoyaltyCardsViewModel : ViewModel() {
    abstract fun state(): StateFlow<LoyaltyCardsState>
}
