package com.mzagorscak.spiza.ui.screens.loyaltyCards.details

import androidx.lifecycle.ViewModel
import com.mzagorscak.spiza.ui.model.PresentableLoyaltyCard
import kotlinx.coroutines.flow.StateFlow

data class LoyaltyCardDetailsState(
    val card: PresentableLoyaltyCard? = null,
    val isLoading: Boolean = true,
    /** The card does not exist (e.g. opened from a stale launcher shortcut after it was deleted). */
    val isCardMissing: Boolean = false,
    val isDeleting: Boolean = false,
    /** One-shot: this screen deleted the card, the screen closes. */
    val isDeleted: Boolean = false,
    /** Transient: the delete failed; shown once and cleared with [LoyaltyCardDetailsViewModel.onErrorShown]. */
    val hasError: Boolean = false,
)

abstract class LoyaltyCardDetailsViewModel : ViewModel() {
    abstract fun state(): StateFlow<LoyaltyCardDetailsState>

    abstract fun deleteCard()

    abstract fun onErrorShown()
}
