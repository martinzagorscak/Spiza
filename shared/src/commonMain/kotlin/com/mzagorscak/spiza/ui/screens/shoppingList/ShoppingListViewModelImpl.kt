package com.mzagorscak.spiza.ui.screens.shoppingList

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.mzagorscak.spiza.domain.model.ArticleItem
import com.mzagorscak.spiza.domain.model.Household
import com.mzagorscak.spiza.domain.usecase.AddArticleItemUseCase
import com.mzagorscak.spiza.domain.usecase.GetCurrentUserProfileUseCase
import com.mzagorscak.spiza.domain.usecase.MoveArticleItemUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveArticleItemsUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveHouseholdsUseCase
import com.mzagorscak.spiza.domain.usecase.ObserveSelectedHouseholdIdUseCase
import com.mzagorscak.spiza.domain.usecase.SetArticleItemCheckedUseCase
import com.mzagorscak.spiza.domain.usecase.SetSelectedHouseholdIdUseCase
import com.mzagorscak.spiza.domain.usecase.UpdateArticleItemTextUseCase
import com.mzagorscak.spiza.ui.model.PresentableSimpleHousehold
import com.mzagorscak.spiza.ui.model.toPresentable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOG_TAG = "Spiza.Household"

private sealed interface Snapshot {
    data object Loading : Snapshot

    data object Failed : Snapshot

    data object NoHousehold : Snapshot

    data class Loaded(
        val households: List<Household>,
        val current: Household,
        val items: List<ArticleItem>,
    ) : Snapshot
}

internal class ShoppingListViewModelImpl(
    private val observeHouseholds: ObserveHouseholdsUseCase,
    private val observeItems: ObserveArticleItemsUseCase,
    observeSelectedHouseholdId: ObserveSelectedHouseholdIdUseCase,
    private val setSelectedHouseholdId: SetSelectedHouseholdIdUseCase,
    private val getCurrentUserProfile: GetCurrentUserProfileUseCase,
    private val addItem: AddArticleItemUseCase,
    private val updateItemText: UpdateArticleItemTextUseCase,
    private val setItemChecked: SetArticleItemCheckedUseCase,
    private val moveItem: MoveArticleItemUseCase,
) : ShoppingListViewModel() {
    private val header = MutableStateFlow(ShoppingListState.Header())
    private val body = MutableStateFlow<ShoppingListState.Body>(ShoppingListState.Body.Loading)
    private val retryTrigger = MutableStateFlow(0)
    private val selectedHouseholdId = observeSelectedHouseholdId()

    private var knownHouseholdIds: Set<String>? = null
    private var latest: Snapshot.Loaded? = null

    init {
        observeSnapshots()
            .onEach(::render)
            .launchIn(viewModelScope)
    }

    override fun headerState(): StateFlow<ShoppingListState.Header> = header.asStateFlow()

    override fun bodyState(): StateFlow<ShoppingListState.Body> = body.asStateFlow()

    override fun onRetryClicked() {
        Logger.i(tag = LOG_TAG) { "Retry clicked" }
        retryTrigger.update { it + 1 }
    }

    override fun onHouseholdSelected(householdId: String) {
        Logger.i(tag = LOG_TAG) { "Household selected: $householdId" }
        persistSelection(householdId)
    }

    private fun persistSelection(householdId: String) {
        viewModelScope.launch { setSelectedHouseholdId(householdId) }
    }

    override fun onAddItem(text: String) {
        val loaded = latest ?: return
        write { addItem(loaded.current.id, text, loaded.items) }
    }

    override fun onItemTextChange(
        itemId: String,
        text: String,
    ) {
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

    override fun onWriteErrorShown() {
        body.update { if (it is ShoppingListState.Body.Content) it.copy(hasWriteError = false) else it }
    }

    // Firestore applies local writes to the listeners immediately, so a failure here means the server rejected it.
    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Logger.e(throwable = error, tag = LOG_TAG) { "Shopping list write failed" }
                body.update { if (it is ShoppingListState.Body.Content) it.copy(hasWriteError = true) else it }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSnapshots(): Flow<Snapshot> =
        retryTrigger.flatMapLatest {
            combine(
                observeHouseholds(),
                selectedHouseholdId,
            ) { households, selectedId ->
                households to (households.firstOrNull { it.id == selectedId } ?: households.firstOrNull())
            }.flatMapLatest { (households, current) ->
                if (current == null) {
                    flowOf(Snapshot.NoHousehold)
                } else {
                    observeItems(current.id).map { Snapshot.Loaded(households, current, it) }
                }
            }.onStart<Snapshot> { emit(Snapshot.Loading) }
                .catch {
                    Logger.e(throwable = it, tag = LOG_TAG) { "Shopping list stream failed" }
                    emit(Snapshot.Failed)
                }
        }

    private fun render(snapshot: Snapshot) {
        latest = snapshot as? Snapshot.Loaded
        when (snapshot) {
            Snapshot.Loading -> body.value = ShoppingListState.Body.Loading
            Snapshot.Failed -> {
                header.value = ShoppingListState.Header()
                body.value = ShoppingListState.Body.Error
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
                    )
                val (done, pending) = snapshot.items.partition { it.isChecked }
                val previousWriteError = (body.value as? ShoppingListState.Body.Content)?.hasWriteError ?: false
                body.value =
                    ShoppingListState.Body.Content(
                        pendingItems = pending.map { it.toPresentable() },
                        doneItems = done.map { it.toPresentable() },
                        hasWriteError = previousWriteError,
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
                Logger.i(tag = LOG_TAG) { "Auto-selecting newly created household ${it.id}" }
                persistSelection(it.id)
            }
    }
}
