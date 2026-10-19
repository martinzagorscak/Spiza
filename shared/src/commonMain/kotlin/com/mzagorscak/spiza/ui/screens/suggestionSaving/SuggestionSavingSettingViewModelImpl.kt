package com.mzagorscak.spiza.ui.screens.suggestionSaving

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.data.suggestion.model.SuggestionSortingType
import com.mzagorscak.spiza.domain.usecase.DeleteAllSuggestionsUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteSuggestionUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsSavingEnabledUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsUseCase
import com.mzagorscak.spiza.domain.usecase.SetSuggestionsSavingEnabledUseCase
import com.mzagorscak.spiza.ui.model.toPresentable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
internal class SuggestionSavingSettingViewModelImpl(
    observeSuggestionsSavingEnabled: ObserveSuggestionsSavingEnabledUseCase,
    observeSuggestions: ObserveSuggestionsUseCase,
    private val setSuggestionsSavingEnabled: SetSuggestionsSavingEnabledUseCase,
    private val deleteSuggestionUseCase: DeleteSuggestionUseCase,
    private val deleteAllSuggestionsUseCase: DeleteAllSuggestionsUseCase,
) : SuggestionSavingSettingViewModel() {
    private val state = MutableStateFlow(SuggestionSavingSettingState())
    private val sortingType = MutableStateFlow(SuggestionSortingType.ALPHABETICAL_ASC)
    private val suggestions = sortingType.flatMapLatest { sortingType ->
        observeSuggestions(sortingType = sortingType)
    }
    private val dialogStatePublisher = MutableStateFlow(SuggestionSavingSettingState.DialogState.Hidden)

    init {
        combine(
            observeSuggestionsSavingEnabled(),
            sortingType,
            suggestions,
            dialogStatePublisher,
        ) { isEnabled, sortingType, suggestions, dialogState ->
            state.update {
                SuggestionSavingSettingState(
                    isEnabled = isEnabled,
                    sortingType = sortingType,
                    suggestions = suggestions.toPresentable(),
                    dialogState = dialogState,
                )
            }
        }.launchIn(viewModelScope)
    }

    override fun state(): StateFlow<SuggestionSavingSettingState> = state.asStateFlow()

    override fun onToggleSavingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            setSuggestionsSavingEnabled(enabled = enabled)
        }
    }

    override fun setSortingType(sortingType: SuggestionSortingType) {
        this.sortingType.value = sortingType
    }

    override fun removeSuggestion(suggestionId: Long) {
        viewModelScope.launch { deleteSuggestionUseCase(id = suggestionId) }
    }

    override fun showRemoveAllSuggestionsConfirmationDialog() {
        dialogStatePublisher.update { SuggestionSavingSettingState.DialogState.RemoveAllSuggestionsConfirmation }
    }

    override fun dismissDialog() {
        dialogStatePublisher.update { SuggestionSavingSettingState.DialogState.Hidden }
    }

    override fun removeAllSuggestions() {
        viewModelScope.launch { deleteAllSuggestionsUseCase() }
        dismissDialog()
    }
}
