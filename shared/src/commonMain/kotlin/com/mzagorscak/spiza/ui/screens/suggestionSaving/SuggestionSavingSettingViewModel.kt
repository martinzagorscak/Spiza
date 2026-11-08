package com.mzagorscak.spiza.ui.screens.suggestionSaving

import androidx.lifecycle.ViewModel
import com.mzagorscak.spiza.domain.model.SuggestionSortingType
import com.mzagorscak.spiza.ui.model.PresentableShoppingItemSuggestion
import kotlinx.coroutines.flow.StateFlow

data class SuggestionSavingSettingState(
    val isEnabled: Boolean = false,
    val sortingType: SuggestionSortingType = SuggestionSortingType.ALPHABETICAL_ASC,
    val suggestions: List<PresentableShoppingItemSuggestion> = emptyList(),
    val dialogState: DialogState = DialogState.Hidden,
    val dialogInput: String = "",
    /** Blocking: the suggestions or the saving setting could not be loaded; the screen offers a retry. */
    val hasLoadingError: Boolean = false,
    /** Transient: a write failed; shown once as a snackbar and cleared with `onErrorShown()`. */
    val hasWriteError: Boolean = false,
) {
    enum class DialogState {
        Hidden,
        RemoveAllSuggestionsConfirmation,
        AddSuggestion,
    }
}

abstract class SuggestionSavingSettingViewModel : ViewModel() {
    abstract fun state(): StateFlow<SuggestionSavingSettingState>

    abstract fun onToggleSavingEnabled(enabled: Boolean)

    abstract fun setSortingType(sortingType: SuggestionSortingType)

    abstract fun removeSuggestion(suggestionId: Long)

    abstract fun showRemoveAllSuggestionsConfirmationDialog()

    abstract fun showAddSuggestionDialog()

    abstract fun onDialogInputChanged(input: String)

    abstract fun addSuggestion()

    abstract fun dismissDialog()

    abstract fun removeAllSuggestions()

    abstract fun retry()

    abstract fun onErrorShown()
}
