package com.mzagorscak.spiza.ui.screens.shoppingList

import androidx.lifecycle.viewModelScope
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.catchAndRecord
import com.mzagorscak.spiza.crash.isQuotaExceeded
import com.mzagorscak.spiza.domain.model.ArticleItem
import com.mzagorscak.spiza.domain.model.Household
import com.mzagorscak.spiza.domain.model.ShoppingItemSuggestion
import com.mzagorscak.spiza.domain.model.findByText
import com.mzagorscak.spiza.domain.usecase.AddArticleItemUseCase
import com.mzagorscak.spiza.domain.usecase.AddOrUpdateSuggestionUseCase
import com.mzagorscak.spiza.domain.usecase.CanAddItemToHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.CanUserCreateHouseholdUseCase
import com.mzagorscak.spiza.domain.usecase.ClearCheckedItemsUseCase
import com.mzagorscak.spiza.domain.usecase.DeleteArticleItemUseCase
import com.mzagorscak.spiza.domain.usecase.GetCurrentUserProfileUseCase
import com.mzagorscak.spiza.domain.usecase.MoveArticleItemUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveArticleItemsUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveHouseholdSelectionUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveShoppingListLockedUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsSavingEnabledUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveSuggestionsUseCase
import com.mzagorscak.spiza.domain.usecase.SetArticleItemCheckedUseCase
import com.mzagorscak.spiza.domain.usecase.SetSelectedHouseholdIdUseCase
import com.mzagorscak.spiza.domain.usecase.ToggleShoppingListLockedUseCase
import com.mzagorscak.spiza.domain.usecase.UpdateArticleItemTextUseCase
import com.mzagorscak.spiza.ui.model.PresentableSimpleHousehold
import com.mzagorscak.spiza.ui.model.toPresentable
import com.mzagorscak.spiza.ui.screens.common.launchSafely
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds

private const val INITIAL_COUNT = 0
private const val CHECKED_ITEMS_COLLAPSE_DELAY_MS = 300L

private sealed interface Snapshot {
    data object Loading : Snapshot

    data class Failed(
        val isQuotaExceeded: Boolean,
    ) : Snapshot

    data object NoHousehold : Snapshot

    data class Loaded(
        val households: List<Household>,
        val current: Household,
        val items: List<ArticleItem>,
    ) : Snapshot
}

internal class ShoppingListViewModelImpl(
    private val observeHouseholdSelection: ObserveHouseholdSelectionUseCase,
    private val observeItems: ObserveArticleItemsUseCase,
    private val setSelectedHouseholdId: SetSelectedHouseholdIdUseCase,
    private val canUserCreateHousehold: CanUserCreateHouseholdUseCase,
    private val getCurrentUserProfile: GetCurrentUserProfileUseCase,
    private val canAddItemToHousehold: CanAddItemToHouseholdUseCase,
    private val addItem: AddArticleItemUseCase,
    private val updateItemText: UpdateArticleItemTextUseCase,
    private val setItemChecked: SetArticleItemCheckedUseCase,
    private val moveItem: MoveArticleItemUseCase,
    private val deleteItem: DeleteArticleItemUseCase,
    private val clearCheckedItems: ClearCheckedItemsUseCase,
    private val addOrUpdateSuggestion: AddOrUpdateSuggestionUseCase,
    observeSuggestionsSavingEnabled: ObserveSuggestionsSavingEnabledUseCase,
    observeSuggestions: ObserveSuggestionsUseCase,
    observeShoppingListLocked: ObserveShoppingListLockedUseCase,
    private val toggleShoppingListLocked: ToggleShoppingListLockedUseCase,
    private val crashLogger: CrashLogger,
) : ShoppingListViewModel() {
    private val header = MutableStateFlow(ShoppingListState.Header())
    private val body = MutableStateFlow<ShoppingListState.Body>(ShoppingListState.Body.Loading)
    private val retryTrigger = MutableStateFlow(INITIAL_COUNT)
    private val areCheckedItemsCollapsedState = MutableStateFlow(true)
    private val dialogStatePublisher = MutableStateFlow(ShoppingListState.DialogState.Hidden)
    private val isSuggestionsSavingEnabled = observeSuggestionsSavingEnabled()

    private var knownHouseholdIds: Set<String>? = null
    private var latest: Snapshot.Loaded? = null

    init {
        combine(
            observeSnapshots(),
            areCheckedItemsCollapsedState,
            dialogStatePublisher,
            observeSuggestions(),
            observeShoppingListLocked(),
        ) { snapshot, areCheckedItemsCollapsed, dialogState, suggestions, isLocked ->
            render(
                snapshot,
                areCheckedItemsCollapsed,
                dialogState,
                suggestions,
                isLocked,
            )
        }.launchIn(viewModelScope)
    }

    override fun headerState(): StateFlow<ShoppingListState.Header> = header.asStateFlow()

    override fun bodyState(): StateFlow<ShoppingListState.Body> = body.asStateFlow()

    override fun retry() {
        retryTrigger.update { it + 1 }
    }

    override fun onHouseholdSelected(householdId: String) {
        persistSelection(householdId)
    }

    private fun persistSelection(householdId: String) {
        launchSafely(crashLogger = crashLogger, context = "Shopping list household selection failed") {
            setSelectedHouseholdId(householdId)
        }
    }

    override fun onAddItem(text: String) {
        if (isLocked()) return
        val loaded = latest ?: return
        write {
            val isNewItem = addItem(householdId = loaded.current.id, text = text, currentItems = loaded.items)
            if (isNewItem && isSuggestionsSavingEnabled.first()) {
                addOrUpdateSuggestion(name = text)
            }
        }
    }

    override fun onItemTextChange(
        itemId: String,
        text: String,
    ) {
        // Not guarded by the lock: the text field is read-only while locked, so this is an edit typed before locking.
        val loaded = latest ?: return
        write { updateItemText(loaded.current.id, itemId, text) }
    }

    override fun onItemCheckedChange(
        itemId: String,
        isChecked: Boolean,
    ) {
        val loaded = latest ?: return
        write { setItemChecked(loaded.current.id, itemId, isChecked) }
    }

    override fun onItemMoved(
        itemId: String,
        toIndex: Int,
    ) {
        val loaded = latest ?: return
        write { moveItem(loaded.current.id, itemId, toIndex, loaded.items) }
    }

    override fun onItemDelete(itemId: String) {
        if (isLocked()) return
        val loaded = latest ?: return
        write { deleteItem(loaded.current.id, itemId) }
    }

    override fun onCheckedItemsCollapse() = areCheckedItemsCollapsedState.update { it.not() }

    override fun showClearCheckedItemsConfirmationDialog() {
        if (isLocked()) return
        dialogStatePublisher.update { ShoppingListState.DialogState.ClearCheckedItemsConfirmation }
    }

    override fun dismissDialog() = dialogStatePublisher.update { ShoppingListState.DialogState.Hidden }

    override fun clearCheckedItems() {
        if (isLocked()) {
            dismissDialog()
            return
        }
        write {
            val loaded = latest ?: return@write
            areCheckedItemsCollapsedState.update { true }
            delay(CHECKED_ITEMS_COLLAPSE_DELAY_MS.milliseconds)
            clearCheckedItems(householdId = loaded.current.id)
        }
        dismissDialog()
    }

    override fun onErrorShown() {
        body.update {
            if (it is ShoppingListState.Body.Content) {
                it.copy(
                    contentErrorState = ShoppingListState.ContentErrorState.None,
                )
            } else {
                it
            }
        }
    }

    override fun onLockToggle() {
        launchSafely(
            crashLogger = crashLogger,
            context = "Shopping list lock toggle failed",
            onError = ::showWriteError,
        ) {
            toggleShoppingListLocked()
        }
    }

    // Adding, deleting and clearing are blocked because the UI hides them while the list is locked.
    private fun isLocked(): Boolean = (body.value as? ShoppingListState.Body.Content)?.isLocked == true

    // Firestore applies local writes to the listeners immediately, so a failure here means the server rejected it.
    private fun write(block: suspend () -> Unit) {
        launchSafely(
            crashLogger = crashLogger,
            context = "Shopping list write failed",
            onError = ::showWriteError,
        ) { block() }
    }

    private fun showWriteError(error: Throwable) {
        body.update {
            if (it is ShoppingListState.Body.Content) {
                it.copy(
                    contentErrorState = if (error.isQuotaExceeded()) {
                        ShoppingListState.ContentErrorState.QuotaExceeded
                    } else {
                        ShoppingListState.ContentErrorState.WriteError
                    },
                )
            } else {
                it
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSnapshots(): Flow<Snapshot> =
        retryTrigger.flatMapLatest {
            channelFlow {
                val selection = observeHouseholdSelection().stateIn(this)
                selection
                    .map { it.current?.id }
                    .distinctUntilChanged()
                    .flatMapLatest { currentId ->
                        if (currentId == null) {
                            flowOf(Snapshot.NoHousehold)
                        } else {
                            combine(selection, observeItems(currentId)) { (households, current), items ->
                                current
                                    ?.takeIf { it.id == currentId }
                                    ?.let { Snapshot.Loaded(households = households, current = it, items = items) }
                            }.filterNotNull()
                        }
                    }.collect { send(it) }
            }.onStart<Snapshot> { emit(Snapshot.Loading) }
                .catchAndRecord(crashLogger = crashLogger, context = "Shopping list stream failed") { error ->
                    emit(Snapshot.Failed(isQuotaExceeded = error.isQuotaExceeded()))
                }
        }

    private fun render(
        snapshot: Snapshot,
        areCheckedItemsCollapsed: Boolean,
        dialogState: ShoppingListState.DialogState,
        suggestions: List<ShoppingItemSuggestion>,
        isLocked: Boolean,
    ) {
        latest = snapshot as? Snapshot.Loaded
        when (snapshot) {
            Snapshot.Loading -> body.value = ShoppingListState.Body.Loading
            is Snapshot.Failed -> {
                header.value = ShoppingListState.Header()
                body.value =
                    ShoppingListState.Body.Error(
                        errorType = if (snapshot.isQuotaExceeded) {
                            ShoppingListState.ErrorType.QuotaExceeded
                        } else {
                            ShoppingListState.ErrorType.Generic
                        },
                    )
            }

            Snapshot.NoHousehold -> {
                selectNewlyCreatedHousehold(emptyList())
                header.value = ShoppingListState.Header()
                body.value = ShoppingListState.Body.NoHousehold
            }

            is Snapshot.Loaded -> {
                selectNewlyCreatedHousehold(snapshot.households)
                header.value =
                    ShoppingListState.Header(
                        currentHouseholdName = snapshot.current.name,
                        otherHouseholds =
                            snapshot.households
                                .filterNot { it.id == snapshot.current.id }
                                .map { PresentableSimpleHousehold(id = it.id, name = it.name) },
                        canUserCreateHousehold = canUserCreateHousehold(snapshot.households.size),
                    )
                val (done, pending) = snapshot.items.partition { it.isChecked }
                val previousError =
                    (body.value as? ShoppingListState.Body.Content)?.contentErrorState ?: ShoppingListState.ContentErrorState.None
                val pendingItems = pending.map { it.toPresentable() }
                val doneItems = done.map { it.toPresentable() }
                body.value =
                    ShoppingListState.Body.Content(
                        currentHouseholdId = snapshot.current.id,
                        pendingItems = pendingItems,
                        doneItems = doneItems,
                        canAddItemToHousehold = canAddItemToHousehold(snapshot.items.count()),
                        suggestions = suggestions.map { it.toPresentable(isEnabled = snapshot.items.findByText(it.name) == null) },
                        areCheckedItemsCollapsed = areCheckedItemsCollapsed,
                        dialogState = dialogState,
                        contentErrorState = previousError,
                        isLocked = isLocked,
                    )
            }
        }
    }

    // The create-household screen has no way to hand its result back, so a household that appears
    // after the first load and is owned by the current user is treated as just created and selected.
    private fun selectNewlyCreatedHousehold(households: List<Household>) {
        val known = knownHouseholdIds
        knownHouseholdIds = households.map { it.id }.toSet()
        if (known == null) return
        val ownerId = getCurrentUserProfile()?.id
        households
            .firstOrNull { it.id !in known && it.ownerId == ownerId }
            ?.let {
                persistSelection(it.id)
            }
    }
}
