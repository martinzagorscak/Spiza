package com.mzagorscak.spiza.ui.screens.loyaltyCards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mzagorscak.spiza.ui.components.FallbackState
import com.mzagorscak.spiza.ui.components.FloatingActionButton
import com.mzagorscak.spiza.ui.components.LoyaltyCard
import com.mzagorscak.spiza.ui.components.SettingsIconButton
import com.mzagorscak.spiza.ui.components.TopBar
import com.mzagorscak.spiza.ui.model.PresentableLoyaltyCard
import com.mzagorscak.spiza.ui.screens.common.MainBottomBar
import com.mzagorscak.spiza.ui.screens.common.MainTab
import com.mzagorscak.spiza.ui.theme.padding400
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.error_generic
import spiza.shared.generated.resources.ic_add_loyalty_card
import spiza.shared.generated.resources.loyalty_cards_empty_description
import spiza.shared.generated.resources.loyalty_cards_empty_title
import spiza.shared.generated.resources.loyalty_cards_shortcut_added
import spiza.shared.generated.resources.loyalty_cards_shortcut_limit_reached
import spiza.shared.generated.resources.loyalty_cards_title

@Composable
fun LoyaltyCardsScreen(
    state: LoyaltyCardsState,
    callbacks: LoyaltyCardsScreenCallbacks,
    sharedTransition: LoyaltyCardSharedTransition,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = when (val stateMessage = state.message) {
        is LoyaltyCardsState.Message.ShortcutLimitReached ->
            stringResource(Res.string.loyalty_cards_shortcut_limit_reached, stateMessage.limit)
        is LoyaltyCardsState.Message.ShortcutAdded ->
            stringResource(Res.string.loyalty_cards_shortcut_added, stateMessage.cardName)
        LoyaltyCardsState.Message.GenericError -> stringResource(Res.string.error_generic)
        LoyaltyCardsState.Message.None -> null
    }
    LaunchedEffect(state.message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            callbacks.onMessageShown()
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(Res.string.loyalty_cards_title),
                trailingContent = {
                    SettingsIconButton(
                        onClick = callbacks.onSettingsClick,
                        tintColor = MaterialTheme.colorScheme.onPrimary,
                    )
                },
            )
        },
        bottomBar = {
            MainBottomBar(
                selectedTab = MainTab.LoyaltyCards,
                onTabClick = callbacks.onTabClick,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                icon = Res.drawable.ic_add_loyalty_card,
                onClick = callbacks.onAddLoyaltyCardClick,
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (state.cards.isEmpty()) {
                FallbackState(
                    title = stringResource(Res.string.loyalty_cards_empty_title),
                    description = stringResource(Res.string.loyalty_cards_empty_description),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding400),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(padding400),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(padding400),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(
                        items = state.cards,
                        key = PresentableLoyaltyCard::id,
                    ) { card ->
                        LoyaltyCard(
                            card = card,
                            onShortcutClick = if (state.areShortcutsSupported) {
                                { callbacks.onToggleShortcut(card.id) }
                            } else {
                                null
                            },
                            modifier = sharedTransition
                                .modifierFor(card.id)
                                .clickable { callbacks.onCardClick(card.id) },
                        )
                    }
                }
            }
        }
    }
}

data class LoyaltyCardsScreenCallbacks(
    val onSettingsClick: () -> Unit,
    val onTabClick: (MainTab) -> Unit,
    val onAddLoyaltyCardClick: () -> Unit,
    val onCardClick: (Long) -> Unit,
    val onToggleShortcut: (Long) -> Unit,
    val onMessageShown: () -> Unit,
)
