package com.mzagorscak.spiza.ui.screens.loyaltyCards.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import com.mzagorscak.spiza.ui.components.Button
import com.mzagorscak.spiza.ui.components.ButtonAppearance
import com.mzagorscak.spiza.ui.components.ButtonSize
import com.mzagorscak.spiza.ui.components.ButtonVariant
import com.mzagorscak.spiza.ui.components.Dialog
import com.mzagorscak.spiza.ui.components.FallbackState
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.components.LoyaltyCard
import com.mzagorscak.spiza.ui.components.TopBar
import com.mzagorscak.spiza.ui.components.common.CTA
import com.mzagorscak.spiza.ui.deviceSettings.KeepScreenBrightness
import com.mzagorscak.spiza.ui.screens.loyaltyCards.LoyaltyCardSharedTransition
import com.mzagorscak.spiza.ui.theme.padding400
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.cancel
import spiza.shared.generated.resources.error_generic
import spiza.shared.generated.resources.ic_back
import spiza.shared.generated.resources.ic_trash
import spiza.shared.generated.resources.loyalty_card_details_card_missing
import spiza.shared.generated.resources.loyalty_card_details_delete_button
import spiza.shared.generated.resources.loyalty_card_details_delete_dialog_description
import spiza.shared.generated.resources.loyalty_card_details_delete_dialog_title
import spiza.shared.generated.resources.loyalty_card_details_title

@Composable
fun LoyaltyCardDetailsScreen(
    state: LoyaltyCardDetailsState,
    callbacks: LoyaltyCardDetailsScreenCallbacks,
    sharedTransition: LoyaltyCardSharedTransition,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) callbacks.onDeleted()
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = if (state.hasError) stringResource(Res.string.error_generic) else null
    LaunchedEffect(state.hasError) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            callbacks.onErrorShown()
        }
    }

    KeepScreenBrightness()

    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(Res.string.loyalty_card_details_title),
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
                trailingContent = {
                    if (state.card != null) {
                        IconButton(
                            resource = Res.drawable.ic_trash,
                            onClick = { showDeleteDialog = true },
                            isEnabled = state.isDeleting.not(),
                            appearance = IconButtonAppearance(
                                variant = IconButtonVariant.IconBased,
                                size = IconButtonSize.Medium,
                                tintColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
            .fillMaxSize()
            .keepScreenOn(),
    ) { paddingValues ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(padding400),
        ) {
            val card = state.card
            if (state.isCardMissing) {
                FallbackState(
                    title = stringResource(Res.string.loyalty_card_details_card_missing),
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (card != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(padding400),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    LoyaltyCard(
                        card = card,
                        modifier = sharedTransition
                            .modifierFor(card.id)
                            .fillMaxWidth(),
                    )

                    Button(
                        label = stringResource(Res.string.loyalty_card_details_delete_button),
                        onClick = { showDeleteDialog = true },
                        isEnabled = state.isDeleting.not(),
                        appearance = ButtonAppearance(
                            variant = ButtonVariant.Secondary,
                            size = ButtonSize.Large,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        Dialog(
            title = stringResource(Res.string.loyalty_card_details_delete_dialog_title),
            description = stringResource(Res.string.loyalty_card_details_delete_dialog_description),
            dismissCTA = CTA(
                label = stringResource(Res.string.cancel),
                onClick = { showDeleteDialog = false },
            ),
            confirmCTA = CTA(
                label = stringResource(Res.string.loyalty_card_details_delete_button),
                onClick = {
                    showDeleteDialog = false
                    callbacks.onDeleteClick()
                },
            ),
        )
    }
}

data class LoyaltyCardDetailsScreenCallbacks(
    val onBackClick: () -> Unit,
    val onDeleteClick: () -> Unit,
    val onDeleted: () -> Unit,
    val onErrorShown: () -> Unit,
)
