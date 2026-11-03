package com.mzagorscak.spiza.ui.screens.loyaltyCards.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import com.mzagorscak.spiza.domain.model.LoyaltyCardFormat
import com.mzagorscak.spiza.ui.components.Button
import com.mzagorscak.spiza.ui.components.ButtonAppearance
import com.mzagorscak.spiza.ui.components.ButtonSize
import com.mzagorscak.spiza.ui.components.ButtonVariant
import com.mzagorscak.spiza.ui.components.ColorPicker
import com.mzagorscak.spiza.ui.components.ColorPickerItem
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.components.LoyaltyCard
import com.mzagorscak.spiza.ui.components.TextField
import com.mzagorscak.spiza.ui.components.TextFieldAppearance
import com.mzagorscak.spiza.ui.components.TextFieldVariant
import com.mzagorscak.spiza.ui.components.TopBar
import com.mzagorscak.spiza.ui.theme.LoyaltyCardColors
import com.mzagorscak.spiza.ui.theme.padding200
import com.mzagorscak.spiza.ui.theme.padding400
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_back
import spiza.shared.generated.resources.loyalty_cards_add_card_name_label
import spiza.shared.generated.resources.loyalty_cards_add_card_name_placeholder
import spiza.shared.generated.resources.loyalty_cards_add_color_label
import spiza.shared.generated.resources.loyalty_cards_add_input_type_camera
import spiza.shared.generated.resources.loyalty_cards_add_input_type_label
import spiza.shared.generated.resources.loyalty_cards_add_input_type_manual
import spiza.shared.generated.resources.loyalty_cards_add_preview_label
import spiza.shared.generated.resources.loyalty_cards_add_save_button
import spiza.shared.generated.resources.loyalty_cards_add_title

@Composable
fun AddLoyaltyCardScreen(
    state: AddLoyaltyCardState,
    callbacks: AddLoyaltyCardScreenCallbacks,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(Res.string.loyalty_cards_add_title),
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
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(padding400),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(padding400)
                .verticalScroll(rememberScrollState()),
        ) {
            TextField(
                text = state.cardName,
                onTextChange = callbacks.onCardNameChange,
                isError = false,
                appearance = TextFieldAppearance(
                    variant = TextFieldVariant.Outlined,
                    singleLine = true,
                    label = stringResource(Res.string.loyalty_cards_add_card_name_label),
                    placeholder = stringResource(Res.string.loyalty_cards_add_card_name_placeholder),
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(padding200)) {
                Text(
                    text = stringResource(Res.string.loyalty_cards_add_color_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                val colorPickerItems = remember(state.selectedColor) {
                    LoyaltyCardColors.palette.mapIndexed { index, color ->
                        ColorPickerItem(
                            id = index.toString(),
                            color = color,
                            isSelected = color == state.selectedColor,
                        )
                    }
                }
                ColorPicker(
                    colors = colorPickerItems,
                    onSelect = { item -> callbacks.onColorSelect(item.color) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(padding200)) {
                Text(
                    text = stringResource(Res.string.loyalty_cards_add_input_type_label),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(padding200),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FilterChip(
                        label = { Text(stringResource(Res.string.loyalty_cards_add_input_type_camera)) },
                        selected = state.inputType == AddLoyaltyCardState.InputType.Camera,
                        onClick = { callbacks.onInputTypeChange(AddLoyaltyCardState.InputType.Camera) },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        label = { Text(stringResource(Res.string.loyalty_cards_add_input_type_manual)) },
                        selected = state.inputType == AddLoyaltyCardState.InputType.Manual,
                        onClick = { callbacks.onInputTypeChange(AddLoyaltyCardState.InputType.Manual) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            when (state.inputType) {
                AddLoyaltyCardState.InputType.Manual -> ManualCodeInput(state = state, callbacks = callbacks)
                AddLoyaltyCardState.InputType.Camera -> CameraCodeInput(state = state, callbacks = callbacks)
            }

            if (state.cardName.isNotBlank() || state.codeText.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(padding200)) {
                    Text(
                        text = stringResource(Res.string.loyalty_cards_add_preview_label),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    LoyaltyCard(card = state.presentableCard)
                }
            }

            Button(
                label = stringResource(Res.string.loyalty_cards_add_save_button),
                onClick = callbacks.onSaveClick,
                isEnabled = state.isSaveEnabled,
                appearance = ButtonAppearance(
                    variant = ButtonVariant.Primary,
                    size = ButtonSize.Large,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

data class AddLoyaltyCardScreenCallbacks(
    val onBackClick: () -> Unit,
    val onCardNameChange: (String) -> Unit,
    val onColorSelect: (Color) -> Unit,
    val onInputTypeChange: (AddLoyaltyCardState.InputType) -> Unit,
    val onCodeChange: (String) -> Unit,
    val onFormatChange: (LoyaltyCardFormat) -> Unit,
    val onCodeScanned: (ScannedCode) -> Unit,
    val onScanAgainClick: () -> Unit,
    val onSaveClick: () -> Unit,
    val onRequestPermission: () -> Unit,
    val onOpenSettings: () -> Unit,
)
