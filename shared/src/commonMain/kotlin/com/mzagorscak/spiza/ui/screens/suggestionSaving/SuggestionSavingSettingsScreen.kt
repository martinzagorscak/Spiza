package com.mzagorscak.spiza.ui.screens.suggestionSaving

import androidx.compose.animation.animateBounds
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.tooling.preview.Preview
import com.mzagorscak.spiza.domain.model.SuggestionSortingType
import com.mzagorscak.spiza.ui.components.Button
import com.mzagorscak.spiza.ui.components.ButtonAppearance
import com.mzagorscak.spiza.ui.components.ButtonSize
import com.mzagorscak.spiza.ui.components.ButtonVariant
import com.mzagorscak.spiza.ui.components.Dialog
import com.mzagorscak.spiza.ui.components.FallbackState
import com.mzagorscak.spiza.ui.components.FloatingActionButton
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.components.TextChip
import com.mzagorscak.spiza.ui.components.ToggleButton
import com.mzagorscak.spiza.ui.components.ToggleButtonVariant
import com.mzagorscak.spiza.ui.components.TopBar
import com.mzagorscak.spiza.ui.components.animatable.TrashIconButton
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
import spiza.shared.generated.resources.error_generic
import spiza.shared.generated.resources.ic_back
import spiza.shared.generated.resources.ic_close
import spiza.shared.generated.resources.ic_plus
import spiza.shared.generated.resources.retry
import spiza.shared.generated.resources.settings_shopping_items_saving
import spiza.shared.generated.resources.settings_shopping_items_saving_add_suggestion_dialog_confirm
import spiza.shared.generated.resources.settings_shopping_items_saving_add_suggestion_dialog_description
import spiza.shared.generated.resources.settings_shopping_items_saving_add_suggestion_dialog_label
import spiza.shared.generated.resources.settings_shopping_items_saving_add_suggestion_dialog_title
import spiza.shared.generated.resources.settings_shopping_items_saving_empty_state_description
import spiza.shared.generated.resources.settings_shopping_items_saving_empty_state_title
import spiza.shared.generated.resources.settings_shopping_items_saving_loading_error_description
import spiza.shared.generated.resources.settings_shopping_items_saving_loading_error_title
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

private const val ERROR_LOTTIE_PATH = "files/lottie_error.json"

@Composable
fun SuggestionSavingSettingsScreen(
    state: SuggestionSavingSettingState,
    callbacks: SuggestionSavingSettingsScreenCallbacks,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val writeErrorMessage = stringResource(Res.string.error_generic)
    LaunchedEffect(state.hasWriteError) {
        if (state.hasWriteError) {
            snackbarHostState.showSnackbar(writeErrorMessage)
            callbacks.onErrorShown()
        }
    }

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
                    // The saved value is unknown while loading failed, so the toggle would show a guess.
                    if (state.hasLoadingError.not()) {
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
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                icon = Res.drawable.ic_plus,
                onClick = callbacks.showAddSuggestionDialog,
                isVisible = state.hasLoadingError.not(),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        if (state.hasLoadingError) {
            FallbackState(
                title = stringResource(Res.string.settings_shopping_items_saving_loading_error_title),
                description = stringResource(Res.string.settings_shopping_items_saving_loading_error_description),
                lottiePath = ERROR_LOTTIE_PATH,
                cta = CTA(
                    label = stringResource(Res.string.retry),
                    onClick = callbacks.onRetryClick,
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(padding400),
            )
        } else if (state.suggestions.isEmpty()) {
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

                    TrashIconButton(
                        onClick = callbacks.showRemoveAllSuggestionsConfirmationDialog,
                        tintColor = MaterialTheme.colorScheme.error,
                    )
                }

                LookaheadScope {
                    FlowRow(
                        verticalArrangement = Arrangement.spacedBy(padding100),
                        horizontalArrangement = Arrangement.spacedBy(padding100),
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    ) {
                        state.suggestions.forEach { suggestion ->
                            key(suggestion.id) {
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
                                    // TextChip fills the available width; wrap it to its content so short names stay short chips.
                                    modifier = Modifier
                                        .width(IntrinsicSize.Max)
                                        .animateBounds(lookaheadScope = this@LookaheadScope),
                                )
                            }
                        }
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
                    isConfirmDestructive = true,
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
    val onRetryClick: () -> Unit,
    val onErrorShown: () -> Unit,
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
                PresentableShoppingItemSuggestion(id = 4, name = "Milk"),
                PresentableShoppingItemSuggestion(id = 5, name = "Whole grain toast bread"),
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
            onRetryClick = {},
            onErrorShown = {},
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
