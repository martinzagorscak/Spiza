package com.mzagorscak.spiza.ui.screens.suggestionSaving

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.catchAndRecord
import com.mzagorscak.spiza.data.suggestion.model.SuggestionSortingType
import com.mzagorscak.spiza.domain.usecase.AddOrUpdateSuggestionUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteAllSuggestionsUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteSuggestionUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsSavingEnabledUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsUseCase
import com.mzagorscak.spiza.domain.usecase.SetSuggestionsSavingEnabledUseCase
import com.mzagorscak.spiza.ui.model.toPresentable
import com.mzagorscak.spiza.ui.screens.common.launchSafely
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update

private const val EMPTY = ""

@OptIn(ExperimentalCoroutinesApi::class)
internal class SuggestionSavingSettingViewModelImpl(
    observeSuggestionsSavingEnabled: ObserveSuggestionsSavingEnabledUseCase,
    observeSuggestions: ObserveSuggestionsUseCase,
    private val setSuggestionsSavingEnabled: SetSuggestionsSavingEnabledUseCase,
    private val deleteSuggestionUseCase: DeleteSuggestionUseCase,
    private val deleteAllSuggestionsUseCase: DeleteAllSuggestionsUseCase,
    private val addOrUpdateSuggestionUseCase: AddOrUpdateSuggestionUseCase,
    private val crashLogger: CrashLogger,
) : SuggestionSavingSettingViewModel() {
    private val state = MutableStateFlow(SuggestionSavingSettingState())
    private val sortingType = MutableStateFlow(SuggestionSortingType.ALPHABETICAL_ASC)
    private val suggestions = sortingType.flatMapLatest { sortingType ->
        observeSuggestions(sortingType = sortingType)
    }
    private val dialogStatePublisher = MutableStateFlow(SuggestionSavingSettingState.DialogState.Hidden)
    private val dialogInputPublisher = MutableStateFlow(EMPTY)

    init {
        combine(
            observeSuggestionsSavingEnabled(),
            sortingType,
            suggestions,
            dialogStatePublisher,
            dialogInputPublisher,
        ) { isEnabled, sortingType, suggestions, dialogState, dialogInput ->
            state.update {
                SuggestionSavingSettingState(
                    isEnabled = isEnabled,
                    sortingType = sortingType,
                    suggestions = suggestions.toPresentable(),
                    dialogState = dialogState,
                    dialogInput = dialogInput,
                )
            }
        }.catchAndRecord(crashLogger = crashLogger, context = "Suggestion saving stream failed")
            .launchIn(viewModelScope)
    }

    override fun state(): StateFlow<SuggestionSavingSettingState> = state.asStateFlow()

    override fun onToggleSavingEnabled(enabled: Boolean) {
        launchSafely(crashLogger = crashLogger, context = "Toggling suggestion saving failed") {
            setSuggestionsSavingEnabled(enabled = enabled)
        }
    }

    override fun setSortingType(sortingType: SuggestionSortingType) {
        this.sortingType.value = sortingType
    }

    override fun removeSuggestion(suggestionId: Long) {
        launchSafely(crashLogger = crashLogger, context = "Deleting suggestion failed") {
            deleteSuggestionUseCase(id = suggestionId)
        }
    }

    override fun showRemoveAllSuggestionsConfirmationDialog() {
        dialogStatePublisher.update { SuggestionSavingSettingState.DialogState.RemoveAllSuggestionsConfirmation }
    }

    override fun showAddSuggestionDialog() {
        dialogInputPublisher.update { EMPTY }
        dialogStatePublisher.update { SuggestionSavingSettingState.DialogState.AddSuggestion }
    }

    override fun onDialogInputChanged(input: String) {
        dialogInputPublisher.update { input }
    }

    override fun addSuggestion() {
        val name = dialogInputPublisher.value.trim()
        if (name.isBlank()) return
        val exists = state.value.suggestions.any { it.name.equals(name, ignoreCase = true) }
        if (exists) {
            dismissDialog()
            return
        }
        launchSafely(crashLogger = crashLogger, context = "Adding suggestion failed") {
            addOrUpdateSuggestionUseCase(name = name)
        }
        dismissDialog()
    }

    override fun dismissDialog() {
        dialogInputPublisher.update { EMPTY }
        dialogStatePublisher.update { SuggestionSavingSettingState.DialogState.Hidden }
    }

    override fun removeAllSuggestions() {
        launchSafely(crashLogger = crashLogger, context = "Deleting all suggestions failed") {
            deleteAllSuggestionsUseCase()
        }
        dismissDialog()
    }
}
