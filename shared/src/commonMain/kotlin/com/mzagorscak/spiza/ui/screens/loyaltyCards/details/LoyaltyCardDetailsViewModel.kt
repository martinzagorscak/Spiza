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
    val areShortcutsSupported: Boolean = false,
    /** How many cards can be pinned as app shortcuts at the same time. */
    val shortcutLimit: Int = 0,
    /** Transient: feedback for a shortcut toggle; cleared with [LoyaltyCardDetailsViewModel.onShortcutMessageShown]. */
    val shortcutMessage: ShortcutMessage = ShortcutMessage.None,
    val dialogState: DialogState = DialogState.Hidden,
) {
    enum class DialogState {
        Hidden,
        DeleteConfirmation,
    }

    sealed interface ShortcutMessage {
        data object None : ShortcutMessage

        data class Added(
            val cardName: String,
        ) : ShortcutMessage

        data class LimitReached(
            val limit: Int,
        ) : ShortcutMessage

        data object GenericError : ShortcutMessage
    }
}

abstract class LoyaltyCardDetailsViewModel : ViewModel() {
    abstract fun state(): StateFlow<LoyaltyCardDetailsState>

    abstract fun showDeleteConfirmationDialog()

    abstract fun dismissDialog()

    abstract fun deleteCard()

    abstract fun onErrorShown()

    abstract fun onToggleShortcut()

    abstract fun onShortcutMessageShown()
}
