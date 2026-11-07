package com.mzagorscak.spiza.ui.screens.loyaltyCards

import androidx.lifecycle.ViewModel
import com.mzagorscak.spiza.ui.model.PresentableLoyaltyCard
import kotlinx.coroutines.flow.StateFlow

data class LoyaltyCardsState(
    val cards: List<PresentableLoyaltyCard> = emptyList(),
    val isLoading: Boolean = false,
    val areShortcutsSupported: Boolean = false,
    val message: Message = Message.None,
) {
    sealed interface Message {
        data object None : Message

        data class ShortcutAdded(
            val cardName: String,
        ) : Message

        data class ShortcutLimitReached(
            val limit: Int,
        ) : Message

        data object GenericError : Message
    }
}

abstract class LoyaltyCardsViewModel : ViewModel() {
    abstract fun state(): StateFlow<LoyaltyCardsState>

    abstract fun onToggleShortcut(cardId: Long)

    abstract fun onMessageShown()
}
