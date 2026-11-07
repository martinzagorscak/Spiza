package com.mzagorscak.spiza.ui.screens.loyaltyCards

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.catchAndRecord
import com.mzagorscak.spiza.domain.model.ShortcutToggleResult
import com.mzagorscak.spiza.domain.usecase.CanUseAppShortcutsUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveLoyaltyCardsUseCase
import com.mzagorscak.spiza.domain.usecase.ToggleLoyaltyCardShortcutUseCase
import com.mzagorscak.spiza.ui.model.toPresentable
import com.mzagorscak.spiza.ui.screens.common.launchSafely
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class LoyaltyCardsViewModelImpl(
    observeLoyaltyCardsUseCase: ObserveLoyaltyCardsUseCase,
    canUseAppShortcutsUseCase: CanUseAppShortcutsUseCase,
    private val toggleLoyaltyCardShortcutUseCase: ToggleLoyaltyCardShortcutUseCase,
    private val crashLogger: CrashLogger,
) : LoyaltyCardsViewModel() {
    private val areShortcutsSupported = canUseAppShortcutsUseCase()
    private val message = MutableStateFlow<LoyaltyCardsState.Message>(LoyaltyCardsState.Message.None)

    private val state: StateFlow<LoyaltyCardsState> =
        combine(
            observeLoyaltyCardsUseCase()
                .map { cards -> cards.toPresentable() }
                .catchAndRecord(crashLogger = crashLogger, context = "Loyalty cards stream failed") {
                    emit(emptyList())
                },
            message,
        ) { cards, message ->
            LoyaltyCardsState(
                cards = cards,
                isLoading = false,
                areShortcutsSupported = areShortcutsSupported,
                message = message,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LoyaltyCardsState(isLoading = true, areShortcutsSupported = areShortcutsSupported),
        )

    override fun state(): StateFlow<LoyaltyCardsState> = state

    override fun onToggleShortcut(cardId: Long) {
        launchSafely(
            crashLogger = crashLogger,
            context = "Toggling loyalty card shortcut failed",
            onError = { message.value = LoyaltyCardsState.Message.GenericError },
        ) {
            when (val result = toggleLoyaltyCardShortcutUseCase(cardId)) {
                is ShortcutToggleResult.LimitReached ->
                    message.value = LoyaltyCardsState.Message.ShortcutLimitReached(limit = result.limit)
                ShortcutToggleResult.Enabled -> {
                    val cardName = state.value.cards
                        .firstOrNull { it.id == cardId }
                        ?.name
                        .orEmpty()
                    message.value = LoyaltyCardsState.Message.ShortcutAdded(cardName = cardName)
                }
                // A card deleted in the meantime disappears from the list on its own; nothing to tell the user.
                ShortcutToggleResult.Disabled, ShortcutToggleResult.CardNotFound -> Unit
            }
        }
    }

    override fun onMessageShown() {
        message.value = LoyaltyCardsState.Message.None
    }
}
