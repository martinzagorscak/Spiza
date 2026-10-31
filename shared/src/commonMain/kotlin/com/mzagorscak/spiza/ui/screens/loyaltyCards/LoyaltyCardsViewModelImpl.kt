package com.mzagorscak.spiza.ui.screens.loyaltyCards

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.catchAndRecord
import com.mzagorscak.spiza.domain.usecase.ObserveLoyaltyCardsUseCase
import com.mzagorscak.spiza.ui.model.toPresentable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class LoyaltyCardsViewModelImpl(
    observeLoyaltyCardsUseCase: ObserveLoyaltyCardsUseCase,
    crashLogger: CrashLogger,
) : LoyaltyCardsViewModel() {
    private val state: StateFlow<LoyaltyCardsState> =
        observeLoyaltyCardsUseCase()
            .map { cards -> LoyaltyCardsState(cards = cards.toPresentable(), isLoading = false) }
            .catchAndRecord(crashLogger = crashLogger, context = "Loyalty cards stream failed") {
                emit(LoyaltyCardsState(isLoading = false))
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = LoyaltyCardsState(isLoading = true),
            )

    override fun state(): StateFlow<LoyaltyCardsState> = state
}
