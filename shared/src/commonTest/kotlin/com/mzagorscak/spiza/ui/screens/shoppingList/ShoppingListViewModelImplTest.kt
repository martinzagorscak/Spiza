package com.mzagorscak.spiza.ui.screens.shoppingList

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.domain.constraints.ConstraintsManagerImpl
import com.mzagorscak.spiza.domain.model.ShoppingItemSuggestion
import com.mzagorscak.spiza.domain.usecase.AddArticleItemUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.AddOrUpdateSuggestionUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.CanAddItemToHouseholdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.CanUserCreateHouseholdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ClearCheckedItemsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.DeleteArticleItemUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.GetCurrentUserProfileUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.MoveArticleItemUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveArticleItemsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveHouseholdSelectionUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveShoppingListLockedUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsSavingEnabledUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.SetArticleItemCheckedUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.SetSelectedHouseholdIdUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.ToggleShoppingListLockedUseCaseImpl
import com.mzagorscak.spiza.domain.usecase.UpdateArticleItemTextUseCaseImpl
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakeHouseholdRepository
import com.mzagorscak.spiza.fakes.FakeSettingsRepository
import com.mzagorscak.spiza.fakes.FakeSuggestionRepository
import com.mzagorscak.spiza.fakes.FakeUserRepository
import com.mzagorscak.spiza.fakes.household
import com.mzagorscak.spiza.fakes.item
import com.mzagorscak.spiza.ui.model.PresentableShoppingItemSuggestion
import com.mzagorscak.spiza.ui.model.PresentableSimpleHousehold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

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

    private fun suggestion(
        id: Long,
        name: String,
    ) = ShoppingItemSuggestion(id = id, name = name, normalizedName = name.lowercase(), useCount = 1, lastUsedAt = 0L)

    private fun ShoppingListViewModel.content(): ShoppingListState.Body.Content {
        val body = bodyState().value
        assertThat(body).isInstanceOf<ShoppingListState.Body.Content>()
        return body as ShoppingListState.Body.Content
    }

    private fun viewModel() =
        ShoppingListViewModelImpl(
            observeHouseholdSelection = ObserveHouseholdSelectionUseCaseImpl(households, settings),
            observeItems = ObserveArticleItemsUseCaseImpl(households),
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
            observeShoppingListLocked = ObserveShoppingListLockedUseCaseImpl(settings),
            toggleShoppingListLocked = ToggleShoppingListLockedUseCaseImpl(settings),
            crashLogger = logger,
        )

    @Test
    fun header_showsCurrentHousehold_otherHouseholds_andThatAnotherCanBeCreated() =
        runTest {
            households.households.value =
                listOf(household(id = "household-1", name = "Home"), household(id = "household-2", name = "Cottage"))

            val header = viewModel().headerState().value

            assertThat(header).isEqualTo(
                ShoppingListState.Header(
                    currentHouseholdName = "Home",
                    otherHouseholds = listOf(PresentableSimpleHousehold(id = "household-2", name = "Cottage")),
                    canUserCreateHousehold = true,
                ),
            )
        }

    @Test
    fun body_showsItemsOfCurrentHousehold_splitIntoPendingAndDone() =
        runTest {
            val body = viewModel().content()

            assertThat(body.currentHouseholdId).isEqualTo("household-1")
            assertThat(body.pendingItems.map { it.id }).containsExactly("milk")
            assertThat(body.doneItems.map { it.id }).containsExactly("bread")
            assertThat(body.canAddItemToHousehold).isTrue()
            assertThat(body.areCheckedItemsCollapsed).isTrue()
            assertThat(body.dialogState).isEqualTo(ShoppingListState.DialogState.Hidden)
        }

    @Test
    fun body_disablesSuggestions_thatAreAlreadyOnTheList() =
        runTest {
            // "milk" matches the pending "Milk" like adding it would, so it must not look addable.
            suggestions.suggestions.value =
                listOf(suggestion(id = 1, name = "milk"), suggestion(id = 2, name = "Bread"), suggestion(id = 3, name = "Eggs"))

            val body = viewModel().content()

            assertThat(body.suggestions).containsExactly(
                PresentableShoppingItemSuggestion(id = 1, name = "milk", isEnabled = false),
                PresentableShoppingItemSuggestion(id = 2, name = "Bread", isEnabled = false),
                PresentableShoppingItemSuggestion(id = 3, name = "Eggs", isEnabled = true),
            )
        }

    @Test
    fun body_showsNoHouseholdState_whenUserHasNoHousehold() =
        runTest {
            households.households.value = emptyList()

            assertThat(viewModel().bodyState().value).isEqualTo(ShoppingListState.Body.NoHousehold)
        }

    @Test
    fun newlyCreatedHousehold_isSelected_whenItAppearsAfterTheFirstLoad() =
        runTest {
            val viewModel = viewModel()

            households.households.value += household(id = "household-new", name = "Cottage")

            assertThat(settings.selectedId.value).isEqualTo("household-new")
            assertThat(viewModel.content().currentHouseholdId).isEqualTo("household-new")
            assertThat(viewModel.headerState().value.currentHouseholdName).isEqualTo("Cottage")
        }

    @Test
    fun householdJoinedLater_isNotSelected_whenItIsOwnedBySomeoneElse() =
        runTest {
            val viewModel = viewModel()

            households.households.value += household(id = "household-joined", name = "Friends", ownerId = "user-2")

            assertThat(settings.selectedId.value).isNull()
            assertThat(viewModel.headerState().value.currentHouseholdName).isEqualTo("Home")
        }

    @Test
    fun onHouseholdSelected_savesSelection_andShowsThatHousehold() =
        runTest {
            households.households.value =
                listOf(household(id = "household-1", name = "Home"), household(id = "household-2", name = "Cottage"))
            val viewModel = viewModel()

            viewModel.onHouseholdSelected("household-2")

            assertThat(settings.selectedId.value).isEqualTo("household-2")
            assertThat(viewModel.content().currentHouseholdId).isEqualTo("household-2")
            assertThat(viewModel.headerState().value.currentHouseholdName).isEqualTo("Cottage")
            assertThat(viewModel.headerState().value.otherHouseholds)
                .containsExactly(PresentableSimpleHousehold(id = "household-1", name = "Home"))
        }

    @Test
    fun householdsUpdate_keepsItemsListener_andShowsNewHeader_whenSelectedHouseholdStaysTheSame() =
        runTest {
            households.households.value =
                listOf(household(id = "household-1", name = "Home"), household(id = "household-2", name = "Cottage"))
            val viewModel = viewModel()

            households.households.value =
                listOf(household(id = "household-1", name = "New home"), household(id = "household-2", name = "Cottage"))
            households.households.value += household(id = "household-joined", name = "Friends", ownerId = "user-2")

            assertThat(households.observedItemsHouseholdIds).containsExactly("household-1")
            assertThat(households.observeHouseholdsCollections).isEqualTo(1)
            assertThat(viewModel.headerState().value.currentHouseholdName).isEqualTo("New home")
            assertThat(viewModel.headerState().value.otherHouseholds).containsExactly(
                PresentableSimpleHousehold(id = "household-2", name = "Cottage"),
                PresentableSimpleHousehold(id = "household-joined", name = "Friends"),
            )
            assertThat(viewModel.content().pendingItems.map { it.id }).containsExactly("milk")
        }

    @Test
    fun onHouseholdSelected_startsItemsListenerForTheNewHousehold() =
        runTest {
            households.households.value =
                listOf(household(id = "household-1", name = "Home"), household(id = "household-2", name = "Cottage"))
            val viewModel = viewModel()

            viewModel.onHouseholdSelected("household-2")

            assertThat(households.observedItemsHouseholdIds).containsExactly("household-1", "household-2")
            assertThat(households.observeHouseholdsCollections).isEqualTo(1)
            assertThat(viewModel.content().currentHouseholdId).isEqualTo("household-2")
        }

    @Test
    fun itemWrites_goToTheSelectedHousehold() =
        runTest {
            households.households.value = listOf(household(id = "household-1"), household(id = "household-2"))
            val viewModel = viewModel()
            viewModel.onHouseholdSelected("household-2")

            viewModel.onAddItem("Eggs")
            viewModel.onItemTextChange(itemId = "milk", text = "Oat milk")
            viewModel.onItemCheckedChange(itemId = "milk", isChecked = true)
            viewModel.onItemMoved(itemId = "milk", toIndex = 0)
            viewModel.onItemDelete("milk")

            assertThat(households.itemWriteHouseholdIds).isEqualTo(List(5) { "household-2" })
        }

    @Test
    fun addItem_addsNewItem_andSavesSuggestion_whenSavingIsEnabled() =
        runTest {
            settings.savingEnabled.value = true

            viewModel().onAddItem("Eggs")

            assertThat(households.addedItems.map { it.text }).containsExactly("Eggs")
            assertThat(suggestions.saved.map { it.name }).containsExactly("Eggs")
        }

    @Test
    fun addItem_doesNotSaveSuggestion_whenSavingIsDisabled() =
        runTest {
            viewModel().onAddItem("Eggs")

            assertThat(households.addedItems.map { it.text }).containsExactly("Eggs")
            assertThat(suggestions.saved).isEmpty()
        }

    @Test
    fun addItem_showsNewItemAmongPendingItems_afterTheExistingOnes() =
        runTest {
            households.items.value += item(id = "eggs", text = "Eggs", position = 3072.0)
            val viewModel = viewModel()

            viewModel.onAddItem("Butter")

            assertThat(viewModel.content().pendingItems.map { it.text }).containsExactly("Milk", "Eggs", "Butter")
        }

    @Test
    fun addItem_doesNotSaveSuggestion_whenTheItemIsAlreadyOnTheList() =
        runTest {
            settings.savingEnabled.value = true

            viewModel().onAddItem(" bread ")

            assertThat(households.checkedChanges).containsExactly("bread" to false)
            assertThat(households.addedItems).isEmpty()
            assertThat(suggestions.saved).isEmpty()
        }

    @Test
    fun onItemTextChange_savesTrimmedText() =
        runTest {
            val viewModel = viewModel()

            viewModel.onItemTextChange(itemId = "milk", text = " Oat milk ")

            assertThat(households.updatedTexts).containsExactly("milk" to "Oat milk")
            assertThat(viewModel.content().pendingItems.map { it.text }).containsExactly("Oat milk")
        }

    @Test
    fun onItemCheckedChange_savesTheCheckedState() =
        runTest {
            val viewModel = viewModel()

            viewModel.onItemCheckedChange(itemId = "milk", isChecked = true)

            assertThat(households.checkedChanges).containsExactly("milk" to true)
            assertThat(viewModel.content().pendingItems).isEmpty()
            assertThat(viewModel.content().doneItems.map { it.id }).containsExactly("milk", "bread")
        }

    @Test
    fun onItemMoved_savesPositionAmongTheShownPendingItems() =
        runTest {
            households.items.value += item(id = "eggs", text = "Eggs", position = 3072.0)
            val viewModel = viewModel()

            viewModel.onItemMoved(itemId = "eggs", toIndex = 0)

            assertThat(households.movedItems.map { it.first }).containsExactly("eggs")
            // Must be before milk, the first pending item.
            assertThat(viewModel.content().pendingItems.map { it.id }).containsExactly("eggs", "milk")
        }

    @Test
    fun onItemDelete_deletesTheItem() =
        runTest {
            val viewModel = viewModel()

            viewModel.onItemDelete("milk")

            assertThat(households.deletedItems).containsExactly("milk")
            assertThat(viewModel.content().pendingItems).isEmpty()
        }

    @Test
    fun onCheckedItemsCollapse_togglesTheCheckedItemsSection() =
        runTest {
            val viewModel = viewModel()

            viewModel.onCheckedItemsCollapse()
            assertThat(viewModel.content().areCheckedItemsCollapsed).isFalse()

            viewModel.onCheckedItemsCollapse()
            assertThat(viewModel.content().areCheckedItemsCollapsed).isTrue()
        }

    @Test
    fun showClearCheckedItemsConfirmationDialog_showsTheConfirmation() =
        runTest {
            val viewModel = viewModel()

            viewModel.showClearCheckedItemsConfirmationDialog()

            assertThat(viewModel.content().dialogState).isEqualTo(ShoppingListState.DialogState.ClearCheckedItemsConfirmation)
        }

    @Test
    fun dismissDialog_hidesTheConfirmation() =
        runTest {
            val viewModel = viewModel()
            viewModel.showClearCheckedItemsConfirmationDialog()

            viewModel.dismissDialog()

            assertThat(viewModel.content().dialogState).isEqualTo(ShoppingListState.DialogState.Hidden)
            assertThat(households.clearedHouseholds).isEmpty()
        }

    @Test
    fun clearCheckedItems_collapsesSectionAndHidesDialog_thenClearsCheckedItemsOfCurrentHousehold() =
        runTest {
            val viewModel = viewModel()
            viewModel.onCheckedItemsCollapse()
            viewModel.showClearCheckedItemsConfirmationDialog()

            viewModel.clearCheckedItems()

            val body = viewModel.content()
            assertThat(body.dialogState).isEqualTo(ShoppingListState.DialogState.Hidden)
            assertThat(body.areCheckedItemsCollapsed).isTrue()
            // Items are cleared only after the section has collapsed.
            assertThat(households.clearedHouseholds).isEmpty()
            assertThat(body.doneItems.map { it.id }).containsExactly("bread")

            advanceUntilIdle()

            assertThat(households.clearedHouseholds).containsExactly("household-1")
            assertThat(viewModel.content().doneItems).isEmpty()
            assertThat(viewModel.content().pendingItems.map { it.id }).containsExactly("milk")
        }

    @Test
    fun body_showsTheListAsLocked_whenItIsLocked() =
        runTest {
            val viewModel = viewModel()
            assertThat(viewModel.content().isLocked).isFalse()

            settings.listLocked.value = true

            assertThat(viewModel.content().isLocked).isTrue()
        }

    @Test
    fun onLockToggle_appliesBothTaps_whenTappedTwiceBeforeTheListUpdates() =
        runTest {
            // A queued dispatcher keeps the screen from re-rendering between the two taps, like a quick double tap.
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onLockToggle()
            viewModel.onLockToggle()
            advanceUntilIdle()

            assertThat(settings.shoppingListLockedWrites).containsExactly(true, false)
            assertThat(viewModel.content().isLocked).isFalse()
        }

    @Test
    fun onAddItem_isIgnored_whenLocked() =
        runTest {
            settings.listLocked.value = true

            viewModel().onAddItem("Eggs")

            assertThat(households.addedItems).isEmpty()
            assertThat(households.checkedChanges).isEmpty()
        }

    // The row flushes an edit still in its debounce when the list gets locked; dropping it would leave the row out of sync.
    @Test
    fun onItemTextChange_savesAnEditTypedBeforeLocking() =
        runTest {
            settings.listLocked.value = true
            val viewModel = viewModel()

            viewModel.onItemTextChange(itemId = "milk", text = "Oat milk")

            assertThat(households.updatedTexts).containsExactly("milk" to "Oat milk")
            assertThat(viewModel.content().pendingItems.map { it.text }).containsExactly("Oat milk")
        }

    @Test
    fun onItemDelete_isIgnored_whenLocked() =
        runTest {
            settings.listLocked.value = true
            val viewModel = viewModel()

            viewModel.onItemDelete("milk")

            assertThat(households.deletedItems).isEmpty()
            assertThat(viewModel.content().pendingItems.map { it.id }).containsExactly("milk")
        }

    @Test
    fun showClearCheckedItemsConfirmationDialog_isIgnored_whenLocked() =
        runTest {
            settings.listLocked.value = true
            val viewModel = viewModel()

            viewModel.showClearCheckedItemsConfirmationDialog()

            assertThat(viewModel.content().dialogState).isEqualTo(ShoppingListState.DialogState.Hidden)
        }

    @Test
    fun clearCheckedItems_doesNotClear_andHidesDialog_whenLockedWhileDialogIsShown() =
        runTest {
            val viewModel = viewModel()
            viewModel.showClearCheckedItemsConfirmationDialog()
            settings.listLocked.value = true

            viewModel.clearCheckedItems()
            advanceUntilIdle()

            assertThat(households.clearedHouseholds).isEmpty()
            assertThat(viewModel.content().doneItems.map { it.id }).containsExactly("bread")
            assertThat(viewModel.content().dialogState).isEqualTo(ShoppingListState.DialogState.Hidden)
        }

    @Test
    fun onItemCheckedChange_savesTheCheckedState_whenLocked() =
        runTest {
            settings.listLocked.value = true
            val viewModel = viewModel()

            viewModel.onItemCheckedChange(itemId = "milk", isChecked = true)

            assertThat(households.checkedChanges).containsExactly("milk" to true)
            assertThat(viewModel.content().doneItems.map { it.id }).containsExactly("milk", "bread")
        }

    @Test
    fun onItemMoved_savesThePosition_whenLocked() =
        runTest {
            households.items.value += item(id = "eggs", text = "Eggs", position = 3072.0)
            settings.listLocked.value = true
            val viewModel = viewModel()

            viewModel.onItemMoved(itemId = "eggs", toIndex = 0)

            assertThat(viewModel.content().pendingItems.map { it.id }).containsExactly("eggs", "milk")
        }

    // region Error handling

    @Test
    fun onLockToggle_showsWriteError_andKeepsTheListUnlocked_whenSavingFails() =
        runTest {
            val failure = IllegalStateException("storage unavailable")
            settings.setShoppingListLockedFailure = failure
            val viewModel = viewModel()

            viewModel.onLockToggle()

            assertThat(viewModel.content().isLocked).isFalse()
            assertThat(viewModel.content().contentErrorState).isEqualTo(ShoppingListState.ContentErrorState.WriteError)
            assertThat(
                logger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo(failure.message)
        }

    @Test
    fun failedWrite_showsWriteError_untilItIsShown() =
        runTest {
            val failure = DataException(DataException.Reason.PermissionDenied, "rejected by rules")
            households.writeFailure = failure
            val viewModel = viewModel()

            viewModel.onItemDelete("milk")

            assertThat(viewModel.content().contentErrorState).isEqualTo(ShoppingListState.ContentErrorState.WriteError)
            // A rejected write is a bug and must be recorded.
            assertThat(
                logger.exceptions
                    .single()
                    .cause
                    ?.message,
            ).isEqualTo(failure.message)

            viewModel.onErrorShown()

            assertThat(viewModel.content().contentErrorState).isEqualTo(ShoppingListState.ContentErrorState.None)
        }

    @Test
    fun failedWrite_showsQuotaExceededError_whenQuotaIsExceeded() =
        runTest {
            households.writeFailure = DataException(DataException.Reason.QuotaExceeded, "quota")
            val viewModel = viewModel()

            viewModel.onItemDelete("milk")

            assertThat(viewModel.content().contentErrorState).isEqualTo(ShoppingListState.ContentErrorState.QuotaExceeded)
        }

    @Test
    fun failedStream_showsGenericError_andRetryRecovers() =
        runTest {
            households.householdsFailure = DataException(DataException.Reason.PermissionDenied, "denied")
            val viewModel = viewModel()

            assertThat(viewModel.bodyState().value)
                .isEqualTo(ShoppingListState.Body.Error(errorType = ShoppingListState.ErrorType.Generic))

            households.householdsFailure = null
            viewModel.retry()

            assertThat(viewModel.content().pendingItems.map { it.id }).containsExactly("milk")
        }

    @Test
    fun householdsStreamFailingAfterLoad_showsGenericError_andRetryRecovers() =
        runTest {
            val viewModel = viewModel()
            assertThat(viewModel.content().pendingItems.map { it.id }).containsExactly("milk")

            households.householdsFailure = DataException(DataException.Reason.PermissionDenied, "denied")
            households.households.value = listOf(household(id = "household-1", name = "Renamed"))

            assertThat(viewModel.bodyState().value)
                .isEqualTo(ShoppingListState.Body.Error(errorType = ShoppingListState.ErrorType.Generic))

            households.householdsFailure = null
            viewModel.retry()

            assertThat(viewModel.content().pendingItems.map { it.id }).containsExactly("milk")
            assertThat(viewModel.headerState().value.currentHouseholdName).isEqualTo("Renamed")
        }

    @Test
    fun failedItemsStream_showsGenericError_andRetryRecovers() =
        runTest {
            households.itemsFailure = DataException(DataException.Reason.PermissionDenied, "denied")
            val viewModel = viewModel()

            assertThat(viewModel.bodyState().value)
                .isEqualTo(ShoppingListState.Body.Error(errorType = ShoppingListState.ErrorType.Generic))

            households.itemsFailure = null
            viewModel.retry()

            assertThat(viewModel.content().pendingItems.map { it.id }).containsExactly("milk")
            assertThat(households.observeHouseholdsCollections).isEqualTo(2)
        }

    @Test
    fun failedStream_showsQuotaExceededError_whenQuotaIsExceeded() =
        runTest {
            households.householdsFailure = DataException(DataException.Reason.QuotaExceeded, "quota")

            val body = viewModel().bodyState().value

            assertThat(body).isEqualTo(ShoppingListState.Body.Error(errorType = ShoppingListState.ErrorType.QuotaExceeded))
            // A used-up quota must be recorded, not only logged.
            assertThat(logger.exceptions).hasSize(1)
        }

    // endregion
}
