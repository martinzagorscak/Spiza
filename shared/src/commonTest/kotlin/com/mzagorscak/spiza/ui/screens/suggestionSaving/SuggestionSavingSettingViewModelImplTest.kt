package com.mzagorscak.spiza.ui.screens.suggestionSaving

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.mzagorscak.spiza.domain.model.ShoppingItemSuggestion
import com.mzagorscak.spiza.domain.model.SuggestionSortingType
import com.mzagorscak.spiza.domain.usecase.AddOrUpdateSuggestionUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.DeleteAllSuggestionsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.DeleteSuggestionUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsSavingEnabledUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.SetSuggestionsSavingEnabledUseCaseImpl
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakeSettingsRepository
import com.mzagorscak.spiza.fakes.FakeSuggestionRepository
import com.mzagorscak.spiza.ui.model.PresentableShoppingItemSuggestion
import com.mzagorscak.spiza.ui.screens.suggestionSaving.SuggestionSavingSettingState.DialogState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private fun suggestion(
    id: Long,
    name: String,
) = ShoppingItemSuggestion(id = id, name = name, normalizedName = name.lowercase(), useCount = 1, lastUsedAt = id)

@OptIn(ExperimentalCoroutinesApi::class)
class SuggestionSavingSettingViewModelImplTest {
    private val settings = FakeSettingsRepository()
    private val suggestions = FakeSuggestionRepository()
    private val logger = FakeCrashLogger()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        SuggestionSavingSettingViewModelImpl(
            observeSuggestionsSavingEnabled = ObserveSuggestionsSavingEnabledUseCaseImpl(settings),
            observeSuggestions = ObserveSuggestionsUseCaseImpl(suggestions),
            setSuggestionsSavingEnabled = SetSuggestionsSavingEnabledUseCaseImpl(settings),
            deleteSuggestionUseCase = DeleteSuggestionUseCaseImpl(suggestions),
            deleteAllSuggestionsUseCase = DeleteAllSuggestionsUseCaseImpl(suggestions),
            addOrUpdateSuggestionUseCase = AddOrUpdateSuggestionUseCaseImpl(suggestions),
            crashLogger = logger,
        )

    @Test
    fun state_showsSavingSettingAndSuggestions_andRequestsAlphabeticalSortingByDefault() =
        runTest {
            settings.savingEnabled.value = true
            suggestions.suggestions.value = listOf(suggestion(id = 1L, name = "Milk"))

            val state = viewModel().state().value

            assertThat(state.isEnabled).isTrue()
            assertThat(state.suggestions).containsExactly(PresentableShoppingItemSuggestion(id = 1L, name = "Milk"))
            assertThat(state.sortingType).isEqualTo(SuggestionSortingType.ALPHABETICAL_ASC)
            assertThat(suggestions.requestedSortingTypes.last()).isEqualTo(SuggestionSortingType.ALPHABETICAL_ASC)
        }

    @Test
    fun onToggleSavingEnabled_savesTheSetting_andShowsItInState() =
        runTest {
            val viewModel = viewModel()

            viewModel.onToggleSavingEnabled(enabled = true)

            assertThat(settings.savingEnabled.value).isTrue()
            assertThat(viewModel.state().value.isEnabled).isTrue()
        }

    @Test
    fun setSortingType_reloadsSuggestionsWithTheNewSorting() =
        runTest {
            val viewModel = viewModel()

            viewModel.setSortingType(SuggestionSortingType.MOST_USED_DESC)

            assertThat(viewModel.state().value.sortingType).isEqualTo(SuggestionSortingType.MOST_USED_DESC)
            assertThat(suggestions.requestedSortingTypes.last()).isEqualTo(SuggestionSortingType.MOST_USED_DESC)
        }

    @Test
    fun removeSuggestion_deletesThatSuggestion() =
        runTest {
            suggestions.suggestions.value = listOf(suggestion(id = 1L, name = "Milk"), suggestion(id = 2L, name = "Bread"))
            val viewModel = viewModel()

            viewModel.removeSuggestion(suggestionId = 1L)

            assertThat(suggestions.deletedIds).containsExactly(1L)
            assertThat(
                viewModel
                    .state()
                    .value.suggestions
                    .map { it.name },
            ).containsExactly("Bread")
        }

    @Test
    fun showRemoveAllSuggestionsConfirmationDialog_showsTheConfirmation() =
        runTest {
            val viewModel = viewModel()

            viewModel.showRemoveAllSuggestionsConfirmationDialog()

            assertThat(viewModel.state().value.dialogState).isEqualTo(DialogState.RemoveAllSuggestionsConfirmation)
        }

    @Test
    fun showAddSuggestionDialog_opensTheDialog_withEmptyInput() =
        runTest {
            val viewModel = viewModel()
            viewModel.onDialogInputChanged("stale")

            viewModel.showAddSuggestionDialog()

            val state = viewModel.state().value
            assertThat(state.dialogState).isEqualTo(DialogState.AddSuggestion)
            assertThat(state.dialogInput).isEqualTo("")
        }

    @Test
    fun onDialogInputChanged_showsTheInput() =
        runTest {
            val viewModel = viewModel()
            viewModel.showAddSuggestionDialog()

            viewModel.onDialogInputChanged("Eggs")

            assertThat(viewModel.state().value.dialogInput).isEqualTo("Eggs")
        }

    @Test
    fun addSuggestion_savesTheTrimmedInput_andClosesTheDialog() =
        runTest {
            val viewModel = viewModel()
            viewModel.showAddSuggestionDialog()
            viewModel.onDialogInputChanged("  Eggs ")

            viewModel.addSuggestion()

            assertThat(suggestions.saved.map { it.name }).containsExactly("Eggs")
            val state = viewModel.state().value
            assertThat(state.suggestions.map { it.name }).containsExactly("Eggs")
            assertThat(state.dialogState).isEqualTo(DialogState.Hidden)
            assertThat(state.dialogInput).isEqualTo("")
        }

    @Test
    fun addSuggestion_closesTheDialogWithoutSaving_whenSuggestionExistsInAnotherCase() =
        runTest {
            suggestions.suggestions.value = listOf(suggestion(id = 1L, name = "Milk"))
            val viewModel = viewModel()
            viewModel.showAddSuggestionDialog()
            viewModel.onDialogInputChanged("milk ")

            viewModel.addSuggestion()

            assertThat(suggestions.saved).isEmpty()
            assertThat(viewModel.state().value.dialogState).isEqualTo(DialogState.Hidden)
        }

    @Test
    fun dismissDialog_hidesTheDialog_andClearsTheInput() =
        runTest {
            val viewModel = viewModel()
            viewModel.showAddSuggestionDialog()
            viewModel.onDialogInputChanged("Eggs")

            viewModel.dismissDialog()

            val state = viewModel.state().value
            assertThat(state.dialogState).isEqualTo(DialogState.Hidden)
            assertThat(state.dialogInput).isEqualTo("")
            assertThat(suggestions.saved).isEmpty()
        }

    @Test
    fun removeAllSuggestions_deletesEverySuggestion_andClosesTheDialog() =
        runTest {
            suggestions.suggestions.value = listOf(suggestion(id = 1L, name = "Milk"))
            val viewModel = viewModel()
            viewModel.showRemoveAllSuggestionsConfirmationDialog()

            viewModel.removeAllSuggestions()

            assertThat(suggestions.deleteAllCalls).isEqualTo(1)
            val state = viewModel.state().value
            assertThat(state.suggestions).isEmpty()
            assertThat(state.dialogState).isEqualTo(DialogState.Hidden)
        }

    // region Error handling

    @Test
    fun addSuggestion_keepsTheDialogOpen_andSavesNothing_whenInputIsBlank() =
        runTest {
            val viewModel = viewModel()
            viewModel.showAddSuggestionDialog()
            viewModel.onDialogInputChanged("   ")

            viewModel.addSuggestion()

            assertThat(suggestions.saved).isEmpty()
            assertThat(viewModel.state().value.dialogState).isEqualTo(DialogState.AddSuggestion)
        }

    @Test
    fun removeSuggestion_keepsTheSuggestion_andShowsWriteErrorUntilItIsShown_whenDeleteFails() =
        runTest {
            val error = IllegalStateException("db closed")
            suggestions.writeFailure = error
            suggestions.suggestions.value = listOf(suggestion(id = 1L, name = "Milk"))
            val viewModel = viewModel()

            viewModel.removeSuggestion(suggestionId = 1L)

            val state = viewModel.state().value
            assertThat(state.hasWriteError).isTrue()
            assertThat(state.suggestions.map { it.name }).containsExactly("Milk")
            assertThat(
                logger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo(error.message)

            viewModel.onErrorShown()

            assertThat(viewModel.state().value.hasWriteError).isFalse()
        }

    @Test
    fun addSuggestion_closesTheDialog_andShowsWriteError_whenSaveFails() =
        runTest {
            suggestions.writeFailure = IllegalStateException("disk full")
            val viewModel = viewModel()
            viewModel.showAddSuggestionDialog()
            viewModel.onDialogInputChanged("Eggs")

            viewModel.addSuggestion()

            val state = viewModel.state().value
            assertThat(state.hasWriteError).isTrue()
            assertThat(state.dialogState).isEqualTo(DialogState.Hidden)
            assertThat(state.suggestions).isEmpty()
        }

    @Test
    fun state_showsLoadingError_whenSuggestionStreamFails() =
        runTest {
            val failure = IllegalStateException("db corrupted")
            suggestions.observeFailure = failure

            val state = viewModel().state().value

            assertThat(state.hasLoadingError).isTrue()
            assertThat(state.hasWriteError).isFalse()
            assertThat(
                logger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo(failure.message)
        }

    @Test
    fun retry_reloadsTheSuggestions_afterTheStreamFailed() =
        runTest {
            suggestions.observeFailure = IllegalStateException("db corrupted")
            suggestions.suggestions.value = listOf(suggestion(id = 1L, name = "Milk"))
            val viewModel = viewModel()
            suggestions.observeFailure = null

            viewModel.retry()

            val state = viewModel.state().value
            assertThat(state.hasLoadingError).isFalse()
            assertThat(state.suggestions.map { it.name }).containsExactly("Milk")
        }

    // endregion
}
