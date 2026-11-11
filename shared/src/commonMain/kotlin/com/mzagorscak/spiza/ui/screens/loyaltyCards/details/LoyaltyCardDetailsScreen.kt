package com.mzagorscak.spiza.ui.screens.loyaltyCards.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import com.mzagorscak.spiza.ui.components.Dialog
import com.mzagorscak.spiza.ui.components.FallbackState
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.components.ListingItem
import com.mzagorscak.spiza.ui.components.LoyaltyCard
import com.mzagorscak.spiza.ui.components.ToggleButton
import com.mzagorscak.spiza.ui.components.ToggleButtonVariant
import com.mzagorscak.spiza.ui.components.TopBar
import com.mzagorscak.spiza.ui.components.animatable.TrashIconButton
import com.mzagorscak.spiza.ui.components.animatable.animatedicons.rememberAnimatedPinPainter
import com.mzagorscak.spiza.ui.components.common.CTA
import com.mzagorscak.spiza.ui.deviceSettings.KeepScreenBrightness
import com.mzagorscak.spiza.ui.model.PresentableLoyaltyCard
import com.mzagorscak.spiza.ui.screens.loyaltyCards.LoyaltyCardSharedTransition
import com.mzagorscak.spiza.ui.theme.padding200
import com.mzagorscak.spiza.ui.theme.padding400
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.cancel
import spiza.shared.generated.resources.error_generic
import spiza.shared.generated.resources.ic_back
import spiza.shared.generated.resources.ic_edit
import spiza.shared.generated.resources.loyalty_card_details_card_missing
import spiza.shared.generated.resources.loyalty_card_details_delete_button
import spiza.shared.generated.resources.loyalty_card_details_delete_dialog_description
import spiza.shared.generated.resources.loyalty_card_details_delete_dialog_title
import spiza.shared.generated.resources.loyalty_card_details_shortcut_description
import spiza.shared.generated.resources.loyalty_card_details_shortcut_label
import spiza.shared.generated.resources.loyalty_card_details_title
import spiza.shared.generated.resources.loyalty_cards_shortcut_added
import spiza.shared.generated.resources.loyalty_cards_shortcut_limit_reached

@Composable
fun LoyaltyCardDetailsScreen(
    state: LoyaltyCardDetailsState,
    callbacks: LoyaltyCardDetailsScreenCallbacks,
    sharedTransition: LoyaltyCardSharedTransition,
    modifier: Modifier = Modifier,
) {
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

    val shortcutMessage = when (val message = state.shortcutMessage) {
        is LoyaltyCardDetailsState.ShortcutMessage.Added ->
            stringResource(Res.string.loyalty_cards_shortcut_added, message.cardName)
        is LoyaltyCardDetailsState.ShortcutMessage.LimitReached ->
            stringResource(Res.string.loyalty_cards_shortcut_limit_reached, message.limit)
        LoyaltyCardDetailsState.ShortcutMessage.GenericError -> stringResource(Res.string.error_generic)
        LoyaltyCardDetailsState.ShortcutMessage.None -> null
    }
    LaunchedEffect(state.shortcutMessage) {
        shortcutMessage?.let {
            snackbarHostState.showSnackbar(it)
            callbacks.onShortcutMessageShown()
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
                            resource = Res.drawable.ic_edit,
                            onClick = callbacks.onEditClick,
                            isEnabled = state.isDeleting.not(),
                            appearance = IconButtonAppearance(
                                variant = IconButtonVariant.IconBased,
                                size = IconButtonSize.Medium,
                                tintColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                        TrashIconButton(
                            onClick = callbacks.showDeleteConfirmationDialog,
                            tintColor = MaterialTheme.colorScheme.onPrimary,
                            isEnabled = state.isDeleting.not(),
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

                    if (state.areShortcutsSupported) {
                        ShortcutSection(
                            card = card,
                            shortcutLimit = state.shortcutLimit,
                            onToggleShortcut = callbacks.onToggleShortcut,
                        )
                    }
                }
            }
        }
    }

    when (state.dialogState) {
        LoyaltyCardDetailsState.DialogState.DeleteConfirmation -> {
            Dialog(
                title = stringResource(Res.string.loyalty_card_details_delete_dialog_title),
                description = stringResource(Res.string.loyalty_card_details_delete_dialog_description),
                dismissCTA = CTA(
                    label = stringResource(Res.string.cancel),
                    onClick = callbacks.onDismissDialog,
                ),
                confirmCTA = CTA(
                    label = stringResource(Res.string.loyalty_card_details_delete_button),
                    onClick = callbacks.onDeleteConfirm,
                ),
                isConfirmDestructive = true,
            )
        }

        LoyaltyCardDetailsState.DialogState.Hidden -> Unit
    }
}

@Composable
private fun ShortcutSection(
    card: PresentableLoyaltyCard,
    shortcutLimit: Int,
    onToggleShortcut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(padding200),
        modifier = modifier.fillMaxWidth(),
    ) {
        ListingItem(
            label = stringResource(Res.string.loyalty_card_details_shortcut_label),
            onClick = onToggleShortcut,
            leadingContent = {
                Icon(
                    painter = rememberAnimatedPinPainter(isPinned = card.isShortcut),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            },
            trailingContent = {
                ToggleButton(
                    checked = card.isShortcut,
                    onCheckedChange = { onToggleShortcut() },
                    variant = ToggleButtonVariant.Custom(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedBorderColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            },
        )
        Text(
            text = stringResource(Res.string.loyalty_card_details_shortcut_description, shortcutLimit),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = padding200),
        )
    }
}

data class LoyaltyCardDetailsScreenCallbacks(
    val onBackClick: () -> Unit,
    val onEditClick: () -> Unit,
    val showDeleteConfirmationDialog: () -> Unit,
    val onDeleteConfirm: () -> Unit,
    val onDismissDialog: () -> Unit,
    val onDeleted: () -> Unit,
    val onErrorShown: () -> Unit,
    val onToggleShortcut: () -> Unit,
    val onShortcutMessageShown: () -> Unit,
)
