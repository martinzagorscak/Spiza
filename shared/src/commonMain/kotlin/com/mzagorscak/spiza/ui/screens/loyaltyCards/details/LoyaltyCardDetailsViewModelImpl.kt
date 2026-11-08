package com.mzagorscak.spiza.ui.screens.loyaltyCards.details

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.catchAndRecord
import com.mzagorscak.spiza.domain.usecase.DeleteLoyaltyCardUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveLoyaltyCardByIdUseCase
import com.mzagorscak.spiza.ui.model.toPresentable
import com.mzagorscak.spiza.ui.screens.common.launchSafely
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal class LoyaltyCardDetailsViewModelImpl(
    private val cardId: Long,
    observeLoyaltyCardByIdUseCase: ObserveLoyaltyCardByIdUseCase,
    private val deleteLoyaltyCardUseCase: DeleteLoyaltyCardUseCase,
    private val crashLogger: CrashLogger,
) : LoyaltyCardDetailsViewModel() {
    private val deletion = MutableStateFlow(Deletion())

    private val state: StateFlow<LoyaltyCardDetailsState> =
        combine(observeLoyaltyCardByIdUseCase(cardId), deletion) { card, progress ->
            // The card may disappear before the delete call returns, so it only counts as missing when this screen
            // is not deleting it; after a failed delete a later disappearance is reported again.
            val isDeletedHere = progress.isDeleting || progress.isDeleted
            LoyaltyCardDetailsState(
                card = card?.toPresentable(),
                isLoading = false,
                isCardMissing = card == null && isDeletedHere.not(),
                isDeleting = progress.isDeleting,
                isDeleted = progress.isDeleted,
                hasError = progress.hasError,
            )
        }.catchAndRecord(crashLogger = crashLogger, context = "Loyalty card details stream failed") {
            emit(LoyaltyCardDetailsState(isLoading = false))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LoyaltyCardDetailsState(isLoading = true),
        )

    override fun state(): StateFlow<LoyaltyCardDetailsState> = state

    override fun deleteCard() {
        val current = deletion.value
        if (current.isDeleting || current.isDeleted) return
        deletion.update { it.copy(isDeleting = true, hasError = false) }
        launchSafely(
            crashLogger = crashLogger,
            context = "Deleting loyalty card failed",
            onError = { deletion.update { it.copy(isDeleting = false, hasError = true) } },
        ) {
            deleteLoyaltyCardUseCase(cardId)
            deletion.update { it.copy(isDeleting = false, isDeleted = true) }
        }
    }

    override fun onErrorShown() {
        deletion.update { it.copy(hasError = false) }
    }

    private data class Deletion(
        val isDeleting: Boolean = false,
        val isDeleted: Boolean = false,
        val hasError: Boolean = false,
    )
}
