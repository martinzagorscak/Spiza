package com.mzagorscak.spiza.ui.screens.loyaltyCards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mzagorscak.spiza.ui.components.FallbackState
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.components.LoyaltyCard
import com.mzagorscak.spiza.ui.components.TopBar
import com.mzagorscak.spiza.ui.model.PresentableLoyaltyCard
import com.mzagorscak.spiza.ui.theme.padding400
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_back
import spiza.shared.generated.resources.ic_plus
import spiza.shared.generated.resources.loyalty_cards_empty_description
import spiza.shared.generated.resources.loyalty_cards_empty_title
import spiza.shared.generated.resources.loyalty_cards_title

@Composable
fun LoyaltyCardsScreen(
    state: LoyaltyCardsState,
    callbacks: LoyaltyCardsScreenCallbacks,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(Res.string.loyalty_cards_title),
                leadingContent = {
                    IconButton(
                        resource = Res.drawable.ic_back,
                        onClick = callbacks.onBackClick,
                        appearance = IconButtonAppearance(
                            variant = IconButtonVariant.IconBased,
                            size = IconButtonSize.Medium,
                            tintColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = callbacks.onAddLoyaltyCardClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_plus),
                    contentDescription = null,
                )
            }
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
                            modifier = Modifier.clickable { callbacks.onCardClick(card.id) },
                        )
                    }
                }
            }
        }
    }
}

data class LoyaltyCardsScreenCallbacks(
    val onBackClick: () -> Unit,
    val onAddLoyaltyCardClick: () -> Unit,
    val onCardClick: (Long) -> Unit,
)
