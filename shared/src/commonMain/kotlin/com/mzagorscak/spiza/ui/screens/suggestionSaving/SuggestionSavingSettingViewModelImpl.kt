package com.mzagorscak.spiza.ui.screens.suggestionSaving

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.catchAndRecord
import com.mzagorscak.spiza.domain.model.SuggestionSortingType
import com.mzagorscak.spiza.domain.usecase.AddOrUpdateSuggestionUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteAllSuggestionsUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteSuggestionUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsSavingEnabledUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsUseCase
import com.mzagorscak.spiza.domain.usecase.SetSuggestionsSavingEnabledUseCase
import com.mzagorscak.spiza.ui.model.PresentableShoppingItemSuggestion
import com.mzagorscak.spiza.ui.model.toPresentable
import com.mzagorscak.spiza.ui.screens.common.launchSafely
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update

private const val EMPTY = ""
private const val INITIAL_COUNT = 0

private sealed interface Snapshot {
    data object Failed : Snapshot

    data class Loaded(
        val isEnabled: Boolean,
        val suggestions: List<PresentableShoppingItemSuggestion>,
    ) : Snapshot
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class SuggestionSavingSettingViewModelImpl(
    private val observeSuggestionsSavingEnabled: ObserveSuggestionsSavingEnabledUseCase,
    private val observeSuggestions: ObserveSuggestionsUseCase,
    private val setSuggestionsSavingEnabled: SetSuggestionsSavingEnabledUseCase,
    private val deleteSuggestionUseCase: DeleteSuggestionUseCase,
    private val deleteAllSuggestionsUseCase: DeleteAllSuggestionsUseCase,
    private val addOrUpdateSuggestionUseCase: AddOrUpdateSuggestionUseCase,
    private val crashLogger: CrashLogger,
) : SuggestionSavingSettingViewModel() {
    private val state = MutableStateFlow(SuggestionSavingSettingState())
    private val sortingType = MutableStateFlow(SuggestionSortingType.ALPHABETICAL_ASC)
    private val retryTrigger = MutableStateFlow(INITIAL_COUNT)
    private val dialogStatePublisher = MutableStateFlow(SuggestionSavingSettingState.DialogState.Hidden)
    private val dialogInputPublisher = MutableStateFlow(EMPTY)

    // Only the data streams are caught (and re-subscribed on retry), so sorting and dialogs keep working after a failure.
    private val snapshots: Flow<Snapshot> =
        retryTrigger.flatMapLatest {
            combine(
                observeSuggestionsSavingEnabled(),
                sortingType.flatMapLatest { sortingType -> observeSuggestions(sortingType = sortingType) },
            ) { isEnabled, suggestions ->
                Snapshot.Loaded(isEnabled = isEnabled, suggestions = suggestions.toPresentable())
            }.catchAndRecord<Snapshot>(crashLogger = crashLogger, context = "Suggestion saving stream failed") {
                emit(Snapshot.Failed)
            }
        }

    init {
        combine(
            snapshots,
            sortingType,
            dialogStatePublisher,
            dialogInputPublisher,
        ) { snapshot, sortingType, dialogState, dialogInput ->
            state.update {
                val loaded = snapshot as? Snapshot.Loaded
                it.copy(
                    isEnabled = loaded?.isEnabled ?: it.isEnabled,
                    sortingType = sortingType,
                    suggestions = loaded?.suggestions ?: emptyList(),
                    dialogState = dialogState,
                    dialogInput = dialogInput,
                    hasLoadingError = loaded == null,
                )
            }
        }.catchAndRecord(crashLogger = crashLogger, context = "Suggestion saving state failed") {
            state.update { it.copy(hasLoadingError = true) }
        }.launchIn(viewModelScope)
    }

    override fun state(): StateFlow<SuggestionSavingSettingState> = state.asStateFlow()

    override fun onToggleSavingEnabled(enabled: Boolean) {
        launchAction(context = "Toggling suggestion saving failed") {
            setSuggestionsSavingEnabled(enabled = enabled)
        }
    }

    override fun setSortingType(sortingType: SuggestionSortingType) {
        this.sortingType.value = sortingType
    }

    override fun removeSuggestion(suggestionId: Long) {
        launchAction(context = "Deleting suggestion failed") {
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
        launchAction(context = "Adding suggestion failed") {
            addOrUpdateSuggestionUseCase(name = name)
        }
        // Closed right away: the dialog is its own window and would hide the error snackbar.
        dismissDialog()
    }

    override fun dismissDialog() {
        dialogInputPublisher.update { EMPTY }
        dialogStatePublisher.update { SuggestionSavingSettingState.DialogState.Hidden }
    }

    override fun removeAllSuggestions() {
        launchAction(context = "Deleting all suggestions failed") {
            deleteAllSuggestionsUseCase()
        }
        dismissDialog()
    }

    override fun retry() {
        retryTrigger.update { it + 1 }
    }

    override fun onErrorShown() {
        state.update { it.copy(hasWriteError = false) }
    }

    private fun launchAction(
        context: String,
        block: suspend () -> Unit,
    ) {
        launchSafely(
            crashLogger = crashLogger,
            context = context,
            onError = { state.update { it.copy(hasWriteError = true) } },
        ) { block() }
    }
}
