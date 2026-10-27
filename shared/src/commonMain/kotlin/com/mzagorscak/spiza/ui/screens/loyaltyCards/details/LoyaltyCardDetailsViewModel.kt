package com.mzagorscak.spiza.ui.screens.loyaltyCards.details

import androidx.lifecycle.ViewModel
import com.mzagorscak.spiza.ui.model.PresentableLoyaltyCard
import kotlinx.coroutines.flow.StateFlow

data class LoyaltyCardDetailsState(
    val card: PresentableLoyaltyCard? = null,
    val isLoading: Boolean = true,
)

abstract class LoyaltyCardDetailsViewModel : ViewModel() {
    abstract fun state(): StateFlow<LoyaltyCardDetailsState>

    abstract fun deleteCard()
}
