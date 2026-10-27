package com.mzagorscak.spiza.ui.screens.loyaltyCards.details

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.domain.usecase.DeleteLoyaltyCardUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveLoyaltyCardByIdUseCase
import com.mzagorscak.spiza.ui.model.toPresentable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class LoyaltyCardDetailsViewModelImpl(
    private val cardId: Long,
    observeLoyaltyCardByIdUseCase: ObserveLoyaltyCardByIdUseCase,
    private val deleteLoyaltyCardUseCase: DeleteLoyaltyCardUseCase,
) : LoyaltyCardDetailsViewModel() {
    private val state: StateFlow<LoyaltyCardDetailsState> =
        observeLoyaltyCardByIdUseCase(cardId)
            .map { card -> LoyaltyCardDetailsState(card = card?.toPresentable(), isLoading = false) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = LoyaltyCardDetailsState(isLoading = true),
            )

    override fun state(): StateFlow<LoyaltyCardDetailsState> = state

    override fun deleteCard() {
        viewModelScope.launch {
            deleteLoyaltyCardUseCase(cardId)
        }
    }
}
