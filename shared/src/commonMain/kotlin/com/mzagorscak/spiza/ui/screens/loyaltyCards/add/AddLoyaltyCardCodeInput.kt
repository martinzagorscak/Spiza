package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.domain.model.PermissionStatus
import com.mzagorscak.spiza.ui.components.Button
import com.mzagorscak.spiza.ui.components.ButtonAppearance
import com.mzagorscak.spiza.ui.components.ButtonSize
import com.mzagorscak.spiza.ui.components.ButtonVariant
import com.mzagorscak.spiza.ui.components.FallbackState
import com.mzagorscak.spiza.ui.components.TextField
import com.mzagorscak.spiza.ui.components.TextFieldAppearance
import com.mzagorscak.spiza.ui.components.TextFieldVariant
import com.mzagorscak.spiza.ui.components.common.CTA
import com.mzagorscak.spiza.ui.model.label
import com.mzagorscak.spiza.ui.model.toDisplayCode
import com.mzagorscak.spiza.ui.theme.padding200
import com.mzagorscak.spiza.ui.theme.padding300
import com.mzagorscak.spiza.ui.theme.padding400
import com.mzagorscak.spiza.ui.theme.roundedShape300
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.loyalty_cards_add_camera_permission_cta
import spiza.shared.generated.resources.loyalty_cards_add_camera_permission_description
import spiza.shared.generated.resources.loyalty_cards_add_camera_permission_title
import spiza.shared.generated.resources.loyalty_cards_add_code_invalid
import spiza.shared.generated.resources.loyalty_cards_add_code_label
import spiza.shared.generated.resources.loyalty_cards_add_code_placeholder
import spiza.shared.generated.resources.loyalty_cards_add_format_hint
import spiza.shared.generated.resources.loyalty_cards_add_format_label
import spiza.shared.generated.resources.loyalty_cards_add_scan_again
import spiza.shared.generated.resources.loyalty_cards_add_scan_hint
import spiza.shared.generated.resources.loyalty_cards_add_scan_unsupported
import spiza.shared.generated.resources.loyalty_cards_add_scanned_code
import spiza.shared.generated.resources.loyalty_cards_add_scanned_title

private val ScannerHeight = 240.dp

@Composable
internal fun ManualCodeInput(
    state: AddLoyaltyCardState,
    callbacks: AddLoyaltyCardScreenCallbacks,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(padding400),
        modifier = modifier.fillMaxWidth(),
    ) {
        TextField(
            text = state.codeText,
            onTextChange = callbacks.onCodeChange,
            isError = state.showCodeError,
            appearance = TextFieldAppearance(
                variant = TextFieldVariant.Outlined,
                singleLine = true,
                label = stringResource(Res.string.loyalty_cards_add_code_label),
                placeholder = stringResource(Res.string.loyalty_cards_add_code_placeholder),
                supportingText = if (state.showCodeError) {
                    stringResource(Res.string.loyalty_cards_add_code_invalid, stringResource(state.format.label))
                } else {
                    null
                },
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )

        Column(verticalArrangement = Arrangement.spacedBy(padding200)) {
            Text(
                text = stringResource(Res.string.loyalty_cards_add_format_label),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.loyalty_cards_add_format_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(padding200),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                LoyaltyCardFormat.entries.forEach { format ->
                    FilterChip(
                        label = { Text(stringResource(format.label)) },
                        selected = state.format == format,
                        onClick = { callbacks.onFormatChange(format) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun CameraCodeInput(
    state: AddLoyaltyCardState,
    callbacks: AddLoyaltyCardScreenCallbacks,
    modifier: Modifier = Modifier,
) {
    when {
        state.permissionStatus !is PermissionStatus.Granted -> {
            FallbackState(
                title = stringResource(Res.string.loyalty_cards_add_camera_permission_title),
                description = stringResource(Res.string.loyalty_cards_add_camera_permission_description),
                cta = CTA(
                    label = stringResource(Res.string.loyalty_cards_add_camera_permission_cta),
                    onClick = callbacks.onOpenSettings,
                ),
                modifier = modifier.fillMaxWidth(),
            )
        }

        state.scanStatus == AddLoyaltyCardState.ScanStatus.Scanned -> {
            ScannedCodeSummary(
                state = state,
                onScanAgainClick = callbacks.onScanAgainClick,
                modifier = modifier,
            )
        }

        else -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(padding300),
                modifier = modifier.fillMaxWidth(),
            ) {
                CodeScanner(
                    onCodeScanned = callbacks.onCodeScanned,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ScannerHeight)
                        .clip(roundedShape300),
                )
                if (state.scanStatus == AddLoyaltyCardState.ScanStatus.Unsupported) {
                    Text(
                        text = stringResource(Res.string.loyalty_cards_add_scan_unsupported),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.loyalty_cards_add_scan_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScannedCodeSummary(
    state: AddLoyaltyCardState,
    onScanAgainClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = roundedShape300,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(padding200),
            modifier = Modifier.padding(padding400),
        ) {
            Text(
                text = stringResource(Res.string.loyalty_cards_add_scanned_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    Res.string.loyalty_cards_add_scanned_code,
                    stringResource(state.format.label),
                    state.codeText.toDisplayCode(),
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                label = stringResource(Res.string.loyalty_cards_add_scan_again),
                onClick = onScanAgainClick,
                appearance = ButtonAppearance(
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Medium,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
