package com.mzagorscak.spiza.ui.screens.shoppingList

import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.domain.constraints.ConstraintsManagerImpl
import com.mzagorscak.spiza.domain.usecase.AddArticleItemUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.AddOrUpdateSuggestionUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.CanAddItemToHouseholdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.CanUserCreateHouseholdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ClearCheckedItemsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.DeleteArticleItemUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.GetCurrentUserProfileUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.MoveArticleItemUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveArticleItemsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveHouseholdsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveSelectedHouseholdIdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsSavingEnabledUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.SetArticleItemCheckedUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.SetSelectedHouseholdIdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.UpdateArticleItemTextUseCaseImpl
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakeHouseholdRepository
import com.mzagorscak.spiza.fakes.FakeSettingsRepository
import com.mzagorscak.spiza.fakes.FakeSuggestionRepository
import com.mzagorscak.spiza.fakes.FakeUserRepository
import com.mzagorscak.spiza.fakes.item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModelImplTest {
    private val households = FakeHouseholdRepository()
    private val settings = FakeSettingsRepository()
    private val suggestions = FakeSuggestionRepository()
    private val logger = FakeCrashLogger()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        households.items.value =
            listOf(
                item(id = "milk", text = "Milk", position = 1024.0),
                item(id = "bread", text = "Bread", isChecked = true, position = 2048.0),
            )
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() =
        ShoppingListViewModelImpl(
            observeHouseholds = ObserveHouseholdsUseCaseImpl(households),
            observeItems = ObserveArticleItemsUseCaseImpl(households),
            observeSelectedHouseholdId = ObserveSelectedHouseholdIdUseCaseImpl(settings),
            setSelectedHouseholdId = SetSelectedHouseholdIdUseCaseImpl(settings),
            canUserCreateHousehold = CanUserCreateHouseholdUseCaseImpl(ConstraintsManagerImpl),
            getCurrentUserProfile = GetCurrentUserProfileUseCaseImpl(FakeUserRepository()),
            canAddItemToHousehold = CanAddItemToHouseholdUseCaseImpl(ConstraintsManagerImpl),
            addItem = AddArticleItemUseCaseImpl(households),
            updateItemText = UpdateArticleItemTextUseCaseImpl(households),
            setItemChecked = SetArticleItemCheckedUseCaseImpl(households),
            moveItem = MoveArticleItemUseCaseImpl(households),
            deleteItem = DeleteArticleItemUseCaseImpl(households),
            clearCheckedItems = ClearCheckedItemsUseCaseImpl(households),
            addOrUpdateSuggestion = AddOrUpdateSuggestionUseCaseImpl(suggestions),
            observeSuggestionsSavingEnabled = ObserveSuggestionsSavingEnabledUseCaseImpl(settings),
            observeSuggestions = ObserveSuggestionsUseCaseImpl(suggestions),
            crashLogger = logger,
        )

    @Test
    fun body_splitsItemsIntoPendingAndDone() =
        runTest {
            val body = assertIs<ShoppingListState.Body.Content>(viewModel().bodyState().value)

            assertEquals(listOf("milk"), body.pendingItems.map { it.id })
            assertEquals(listOf("bread"), body.doneItems.map { it.id })
        }

    @Test
    fun addItem_addsNewItem_andSavesSuggestion_whenSavingIsEnabled() =
        runTest {
            settings.savingEnabled.value = true

            viewModel().onAddItem("Eggs")

            assertEquals(listOf("Eggs"), households.addedItems.map { it.text })
            assertEquals(listOf("Eggs"), suggestions.savedNames)
        }

    @Test
    fun addItem_doesNotSaveSuggestion_whenSavingIsDisabled() =
        runTest {
            viewModel().onAddItem("Eggs")

            assertEquals(listOf("Eggs"), households.addedItems.map { it.text })
            assertTrue(suggestions.savedNames.isEmpty())
        }

    @Test
    fun addItem_unchecksTheExistingDoneItem_insteadOfAddingADuplicate() =
        runTest {
            viewModel().onAddItem(" bread ")

            assertEquals(listOf("bread" to false), households.checkedChanges)
            assertTrue(households.addedItems.isEmpty())
        }

    @Test
    fun addItem_movesTheExistingPendingItem_insteadOfAddingADuplicate() =
        runTest {
            viewModel().onAddItem("MILK")

            assertEquals(listOf("milk"), households.movedItems.map { it.first })
            assertTrue(households.addedItems.isEmpty())
        }

    @Test
    fun failedWrite_showsWriteError_untilItIsShown() =
        runTest {
            households.writeFailure = DataException(DataException.Reason.PermissionDenied, "rejected by rules")
            val viewModel = viewModel()

            viewModel.onItemDelete("milk")

            val body = assertIs<ShoppingListState.Body.Content>(viewModel.bodyState().value)
            assertEquals(ShoppingListState.ContentErrorState.WriteError, body.contentErrorState)
            assertEquals(1, logger.exceptions.size, "A rejected write is a bug and must be recorded")

            viewModel.onErrorShown()

            val shown = assertIs<ShoppingListState.Body.Content>(viewModel.bodyState().value)
            assertEquals(ShoppingListState.ContentErrorState.None, shown.contentErrorState)
        }

    @Test
    fun failedStream_showsErrorState_andRetryRecovers() =
        runTest {
            households.householdsFailure = IllegalStateException("listener failed")
            val viewModel = viewModel()

            assertIs<ShoppingListState.Body.Error>(viewModel.bodyState().value)

            households.householdsFailure = null
            viewModel.retry()

            assertIs<ShoppingListState.Body.Content>(viewModel.bodyState().value)
        }

    @Test
    fun noHouseholds_showsNoHouseholdState() =
        runTest {
            households.households.value = emptyList()

            assertIs<ShoppingListState.Body.NoHousehold>(viewModel().bodyState().value)
        }

    @Test
    fun failedStream_showsQuotaExceededError_whenQuotaIsExceeded() =
        runTest {
            households.householdsFailure = DataException(DataException.Reason.QuotaExceeded, "quota")

            val body = viewModel().bodyState().value

            assertEquals(ShoppingListState.Body.Error(errorType = ShoppingListState.ErrorType.QuotaExceeded), body)
            assertEquals(1, logger.exceptions.size, "A used-up quota must be recorded, not only logged")
        }

    @Test
    fun failedStream_showsGenericError_whenFailureIsNotQuota() =
        runTest {
            households.householdsFailure = DataException(DataException.Reason.PermissionDenied, "denied")

            val body = viewModel().bodyState().value

            assertEquals(ShoppingListState.Body.Error(errorType = ShoppingListState.ErrorType.Generic), body)
        }

    @Test
    fun failedStream_recoversToContent_whenRetriedAfterQuotaReset() =
        runTest {
            households.householdsFailure = DataException(DataException.Reason.QuotaExceeded, "quota")
            val viewModel = viewModel()
            assertIs<ShoppingListState.Body.Error>(viewModel.bodyState().value)

            households.householdsFailure = null
            viewModel.retry()

            assertIs<ShoppingListState.Body.Content>(viewModel.bodyState().value)
        }

    @Test
    fun failedWrite_showsQuotaExceededError_untilItIsShown() =
        runTest {
            households.writeFailure = DataException(DataException.Reason.QuotaExceeded, "quota")
            val viewModel = viewModel()

            viewModel.onItemDelete("milk")

            val body = assertIs<ShoppingListState.Body.Content>(viewModel.bodyState().value)
            assertEquals(ShoppingListState.ContentErrorState.QuotaExceeded, body.contentErrorState)
            assertEquals(1, logger.exceptions.size)

            viewModel.onErrorShown()

            val shown = assertIs<ShoppingListState.Body.Content>(viewModel.bodyState().value)
            assertEquals(ShoppingListState.ContentErrorState.None, shown.contentErrorState)
        }

    @Test
    fun failedWrite_showsWriteError_whenQuotaExceededIsNotTheCause() =
        runTest {
            households.writeFailure = IllegalStateException("bug")
            val viewModel = viewModel()

            viewModel.onItemDelete("milk")

            val body = assertIs<ShoppingListState.Body.Content>(viewModel.bodyState().value)
            assertEquals(ShoppingListState.ContentErrorState.WriteError, body.contentErrorState)
        }
}
