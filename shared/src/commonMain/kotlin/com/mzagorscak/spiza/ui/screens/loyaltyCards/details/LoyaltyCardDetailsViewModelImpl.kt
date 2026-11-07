package com.mzagorscak.spiza.ui.screens.loyaltyCards.details

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.catchAndRecord
import com.mzagorscak.spiza.domain.usecase.DeleteLoyaltyCardUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveLoyaltyCardByIdUseCase
import com.mzagorscak.spiza.ui.model.toPresentable
import com.mzagorscak.spiza.ui.screens.common.launchSafely
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class LoyaltyCardDetailsViewModelImpl(
    private val cardId: Long,
    observeLoyaltyCardByIdUseCase: ObserveLoyaltyCardByIdUseCase,
    private val deleteLoyaltyCardUseCase: DeleteLoyaltyCardUseCase,
    private val crashLogger: CrashLogger,
) : LoyaltyCardDetailsViewModel() {
    private var isDeleteRequested = false

    private val state: StateFlow<LoyaltyCardDetailsState> =
        observeLoyaltyCardByIdUseCase(cardId)
            .map { card ->
                LoyaltyCardDetailsState(
                    card = card?.toPresentable(),
                    isLoading = false,
                    isCardMissing = card == null && isDeleteRequested.not(),
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
        isDeleteRequested = true
        launchSafely(crashLogger = crashLogger, context = "Deleting loyalty card failed") {
            deleteLoyaltyCardUseCase(cardId)
        }
    }
}
