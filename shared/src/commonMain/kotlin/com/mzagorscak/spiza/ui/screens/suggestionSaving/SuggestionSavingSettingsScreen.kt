package com.mzagorscak.spiza.ui.screens.suggestionSaving

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.data.suggestion.model.SuggestionSortingType
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
import com.mzagorscak.spiza.ui.components.TextChip
import com.mzagorscak.spiza.ui.components.ToggleButton
import com.mzagorscak.spiza.ui.components.ToggleButtonVariant
import com.mzagorscak.spiza.ui.components.TopBar
import com.mzagorscak.spiza.ui.components.common.CTA
import com.mzagorscak.spiza.ui.model.PresentableShoppingItemSuggestion
import com.mzagorscak.spiza.ui.theme.TheAppTheme
import com.mzagorscak.spiza.ui.theme.padding100
import com.mzagorscak.spiza.ui.theme.padding200
import com.mzagorscak.spiza.ui.theme.padding400
import com.mzagorscak.spiza.ui.theme.padding600
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.cancel
import spiza.shared.generated.resources.ic_back
import spiza.shared.generated.resources.ic_close
import spiza.shared.generated.resources.ic_plus
import spiza.shared.generated.resources.ic_trash
import spiza.shared.generated.resources.settings_shopping_items_saving
import spiza.shared.generated.resources.settings_shopping_items_saving_add_suggestion_dialog_confirm
import spiza.shared.generated.resources.settings_shopping_items_saving_add_suggestion_dialog_description
import spiza.shared.generated.resources.settings_shopping_items_saving_add_suggestion_dialog_label
import spiza.shared.generated.resources.settings_shopping_items_saving_add_suggestion_dialog_title
import spiza.shared.generated.resources.settings_shopping_items_saving_empty_state_description
import spiza.shared.generated.resources.settings_shopping_items_saving_empty_state_title
import spiza.shared.generated.resources.settings_shopping_items_saving_remove_all_suggestions_dialog_confirm
import spiza.shared.generated.resources.settings_shopping_items_saving_remove_all_suggestions_dialog_description
import spiza.shared.generated.resources.settings_shopping_items_saving_remove_all_suggestions_dialog_title
import spiza.shared.generated.resources.settings_shopping_items_saving_sorting_alphabetical_asc
import spiza.shared.generated.resources.settings_shopping_items_saving_sorting_alphabetical_desc
import spiza.shared.generated.resources.settings_shopping_items_saving_sorting_label
import spiza.shared.generated.resources.settings_shopping_items_saving_sorting_last_used_asc
import spiza.shared.generated.resources.settings_shopping_items_saving_sorting_last_used_desc
import spiza.shared.generated.resources.settings_shopping_items_saving_sorting_most_used_asc
import spiza.shared.generated.resources.settings_shopping_items_saving_sorting_most_used_desc

private const val GRID_COLUMNS = 3

@Composable
fun SuggestionSavingSettingsScreen(
    state: SuggestionSavingSettingState,
    callbacks: SuggestionSavingSettingsScreenCallbacks,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(Res.string.settings_shopping_items_saving),
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
                    ToggleButton(
                        checked = state.isEnabled,
                        onCheckedChange = callbacks.onToggleSavingEnabled,
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
        },
        floatingActionButton = {
            IconButton(
                resource = Res.drawable.ic_plus,
                onClick = callbacks.showAddSuggestionDialog,
                appearance = IconButtonAppearance(
                    variant = IconButtonVariant.IconBased,
                    size = IconButtonSize.Large,
                    tintColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        if (state.suggestions.isEmpty()) {
            FallbackState(
                title = stringResource(Res.string.settings_shopping_items_saving_empty_state_title),
                description = stringResource(Res.string.settings_shopping_items_saving_empty_state_description),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(padding400),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(vertical = padding400, horizontal = padding200),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = padding600),
                ) {
                    SortingTypeSelector(
                        sortingType = state.sortingType,
                        onItemClick = callbacks.onSortingTypeChange,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        resource = Res.drawable.ic_trash,
                        onClick = callbacks.showRemoveAllSuggestionsConfirmationDialog,
                        appearance = IconButtonAppearance(
                            variant = IconButtonVariant.IconBased,
                            size = IconButtonSize.Medium,
                            tintColor = MaterialTheme.colorScheme.error,
                        ),
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(count = GRID_COLUMNS),
                    verticalArrangement = Arrangement.spacedBy(padding100),
                    horizontalArrangement = Arrangement.spacedBy(padding100),
                ) {
                    items(items = state.suggestions, key = PresentableShoppingItemSuggestion::id) { suggestion ->
                        TextChip(
                            text = suggestion.name,
                            trailingContent = {
                                IconButton(
                                    resource = Res.drawable.ic_close,
                                    onClick = { callbacks.onRemoveSuggestion(suggestion.id) },
                                    appearance = IconButtonAppearance(
                                        variant = IconButtonVariant.IconBased,
                                        size = IconButtonSize.Small,
                                        tintColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                )
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }

        when (state.dialogState) {
            SuggestionSavingSettingState.DialogState.AddSuggestion -> {
                Dialog(
                    title = stringResource(Res.string.settings_shopping_items_saving_add_suggestion_dialog_title),
                    description = stringResource(Res.string.settings_shopping_items_saving_add_suggestion_dialog_description),
                    textInput = state.dialogInput,
                    onTextInputChange = callbacks.onDialogInputChanged,
                    label = stringResource(Res.string.settings_shopping_items_saving_add_suggestion_dialog_label),
                    trailingIcon = {
                        if (state.dialogInput.isNotEmpty()) {
                            IconButton(
                                resource = Res.drawable.ic_close,
                                onClick = { callbacks.onDialogInputChanged("") },
                                appearance = IconButtonAppearance(
                                    variant = IconButtonVariant.IconBased,
                                    size = IconButtonSize.Medium,
                                    tintColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    },
                    dismissCTA = CTA(
                        label = stringResource(Res.string.cancel),
                        onClick = callbacks.onDismissDialog,
                    ),
                    confirmCTA = CTA(
                        label = stringResource(Res.string.settings_shopping_items_saving_add_suggestion_dialog_confirm),
                        onClick = callbacks.onAddSuggestion,
                        isEnabled = state.dialogInput.isNotBlank(),
                    ),
                )
            }

            SuggestionSavingSettingState.DialogState.RemoveAllSuggestionsConfirmation -> {
                Dialog(
                    title = stringResource(Res.string.settings_shopping_items_saving_remove_all_suggestions_dialog_title),
                    description = stringResource(Res.string.settings_shopping_items_saving_remove_all_suggestions_dialog_description),
                    dismissCTA = CTA(
                        label = stringResource(Res.string.cancel),
                        onClick = callbacks.onDismissDialog,
                    ),
                    confirmCTA = CTA(
                        label = stringResource(Res.string.settings_shopping_items_saving_remove_all_suggestions_dialog_confirm),
                        onClick = callbacks.removeAllSuggestions,
                    ),
                )
            }

            SuggestionSavingSettingState.DialogState.Hidden -> Unit
        }
    }
}

@Composable
private fun SortingTypeSelector(
    sortingType: SuggestionSortingType,
    onItemClick: (SuggestionSortingType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(Res.string.settings_shopping_items_saving_sorting_label),
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(modifier = Modifier.width(padding100))
        Box {
            Button(
                label = sortingType.displayName(),
                onClick = { isDropdownExpanded = true },
                appearance = ButtonAppearance(
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Medium,
                ),
            )
            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
            ) {
                SuggestionSortingType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = type.displayName(),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        onClick = {
                            isDropdownExpanded = false
                            onItemClick(type)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionSortingType.displayName(): String =
    when (this) {
        SuggestionSortingType.ALPHABETICAL_ASC -> stringResource(Res.string.settings_shopping_items_saving_sorting_alphabetical_asc)
        SuggestionSortingType.ALPHABETICAL_DESC -> stringResource(Res.string.settings_shopping_items_saving_sorting_alphabetical_desc)
        SuggestionSortingType.LAST_USED_ASC -> stringResource(Res.string.settings_shopping_items_saving_sorting_last_used_asc)
        SuggestionSortingType.LAST_USED_DESC -> stringResource(Res.string.settings_shopping_items_saving_sorting_last_used_desc)
        SuggestionSortingType.MOST_USED_ASC -> stringResource(Res.string.settings_shopping_items_saving_sorting_most_used_asc)
        SuggestionSortingType.MOST_USED_DESC -> stringResource(Res.string.settings_shopping_items_saving_sorting_most_used_desc)
    }

data class SuggestionSavingSettingsScreenCallbacks(
    val onBackClick: () -> Unit,
    val onToggleSavingEnabled: (Boolean) -> Unit,
    val onSortingTypeChange: (SuggestionSortingType) -> Unit,
    val onRemoveSuggestion: (Long) -> Unit,
    val showRemoveAllSuggestionsConfirmationDialog: () -> Unit = {},
    val showAddSuggestionDialog: () -> Unit = {},
    val onDialogInputChanged: (String) -> Unit = {},
    val onAddSuggestion: () -> Unit = {},
    val onDismissDialog: () -> Unit = {},
    val removeAllSuggestions: () -> Unit,
)

@Composable
private fun SuggestionSavingSettingsScreenLayoutPreview() {
    SuggestionSavingSettingsScreen(
        state = SuggestionSavingSettingState(
            isEnabled = true,
            suggestions = listOf(
                PresentableShoppingItemSuggestion(id = 1, name = "Suggestion 1"),
                PresentableShoppingItemSuggestion(id = 2, name = "Suggestion 2"),
                PresentableShoppingItemSuggestion(id = 3, name = "Suggestion 3"),
            ),
        ),
        callbacks = SuggestionSavingSettingsScreenCallbacks(
            onBackClick = {},
            onToggleSavingEnabled = {},
            onSortingTypeChange = {},
            onRemoveSuggestion = {},
            showRemoveAllSuggestionsConfirmationDialog = {},
            showAddSuggestionDialog = {},
            onDialogInputChanged = {},
            onAddSuggestion = {},
            onDismissDialog = {},
            removeAllSuggestions = {},
        ),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PreviewLightModeSuggestionSavingSettingsScreen() {
    TheAppTheme {
        SuggestionSavingSettingsScreenLayoutPreview()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewDarkModeSuggestionSavingSettingsScreen() {
    TheAppTheme(darkTheme = true) {
        SuggestionSavingSettingsScreenLayoutPreview()
    }
}
