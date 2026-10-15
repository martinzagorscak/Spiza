package com.mzagorscak.spiza.ui.screens.suggestionSaving

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.data.suggestion.model.SuggestionSortingType
import com.mzagorscak.spiza.domain.usecase.DeleteSuggestionUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsSavingEnabledUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsUseCase
import com.mzagorscak.spiza.domain.usecase.SetSuggestionsSavingEnabledUseCase
import com.mzagorscak.spiza.ui.model.toPresentable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
internal class SuggestionSavingSettingViewModelImpl(
    observeSuggestionsSavingEnabled: ObserveSuggestionsSavingEnabledUseCase,
    observeSuggestions: ObserveSuggestionsUseCase,
    private val setSuggestionsSavingEnabled: SetSuggestionsSavingEnabledUseCase,
    private val deleteSuggestionUseCase: DeleteSuggestionUseCase,
) : SuggestionSavingSettingViewModel() {
    private val state = MutableStateFlow(SuggestionSavingSettingState())
    private val sortingType = MutableStateFlow(SuggestionSortingType.ALPHABETICAL_ASC)

    init {
        observeSuggestionsSavingEnabled()
            .onEach { isEnabled -> state.update { it.copy(isEnabled = isEnabled) } }
            .launchIn(viewModelScope)

        sortingType
            .flatMapLatest { sortingType ->
                observeSuggestions(sortingType = sortingType)
                    .onEach { suggestions ->
                        state.update {
                            it.copy(
                                sortingType = sortingType,
                                suggestions = suggestions.toPresentable(),
                            )
                        }
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

    override fun onRemoveSuggestion(suggestionId: Long) {
        viewModelScope.launch {
            deleteSuggestionUseCase(id = suggestionId)
        }
    }
}
