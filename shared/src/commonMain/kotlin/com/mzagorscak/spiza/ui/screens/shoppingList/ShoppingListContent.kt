package com.mzagorscak.spiza.ui.screens.shoppingList

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mzagorscak.spiza.ui.animatedicons.rememberAnimatedChevronPainter
import com.mzagorscak.spiza.ui.animatedicons.rememberAnimatedLockPainter
import com.mzagorscak.spiza.ui.components.Draggable
import com.mzagorscak.spiza.ui.components.EditableCheckListItem
import com.mzagorscak.spiza.ui.components.FallbackState
import com.mzagorscak.spiza.ui.components.IconButton
import com.mzagorscak.spiza.ui.components.IconButtonAppearance
import com.mzagorscak.spiza.ui.components.IconButtonSize
import com.mzagorscak.spiza.ui.components.IconButtonVariant
import com.mzagorscak.spiza.ui.model.PresentableArticleItem
import com.mzagorscak.spiza.ui.theme.padding1600
import com.mzagorscak.spiza.ui.theme.padding200
import com.mzagorscak.spiza.ui.theme.padding400
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.ic_trash
import spiza.shared.generated.resources.shopping_list_add_item_placeholder
import spiza.shared.generated.resources.shopping_list_done_section
import spiza.shared.generated.resources.shopping_list_empty_description
import spiza.shared.generated.resources.shopping_list_empty_locked_description
import spiza.shared.generated.resources.shopping_list_empty_title
import spiza.shared.generated.resources.shopping_list_pending_section
import kotlin.time.Duration.Companion.milliseconds

private const val TEXT_COMMIT_DEBOUNCE_MS = 600L
private const val ARTICLE_TEXT_MAX_LENGTH = 100
private const val OPTIMISTIC_ORDER_TIMEOUT_MS = 3_000L
private const val PENDING_HEADER_KEY = "pending-header"
private const val EMPTY_KEY = "empty"
private const val ADD_ITEM_KEY = "add-item"
private const val DONE_HEADER_KEY = "done-header"
private const val DONE_ITEM_TEMPLATE_KEY = "done-item"

private val AutoScrollEdgeSize = 64.dp
private val AutoScrollStep = 8.dp

@Composable
fun ShoppingListContent(
    state: ShoppingListState.Body.Content,
    callbacks: ShoppingListScreenCallbacks,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val pending = remember { mutableStateListOf<PresentableArticleItem>().apply { addAll(state.pendingItems) } }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    // Outside of a drag the list is rendered straight from the state, so an item that was just checked can never
    // be in both the pending and done sections. While dragging, a local copy is reordered instead.
    val stateOrder = state.pendingItems.map { it.id }
    val pendingIds = stateOrder.toSet()
    // After a drop the new order is kept until the state confirms it, otherwise the item would jump back to its old
    // position and animate to the new one once the update arrives.
    var droppedOrder by remember { mutableStateOf<List<String>?>(null) }
    val optimisticOrder = droppedOrder?.takeIf { it.toSet() == pendingIds }
    val displayedPending = when {
        draggedKey != null -> pending.filter { it.id in pendingIds }
        optimisticOrder != null -> {
            val byId = state.pendingItems.associateBy { it.id }
            optimisticOrder.map { byId.getValue(it) }
        }

        else -> state.pendingItems
    }

    LaunchedEffect(stateOrder, droppedOrder) {
        if (droppedOrder == stateOrder) droppedOrder = null
    }
    // Gives up on the optimistic order if the move was never confirmed (e.g. the write failed).
    LaunchedEffect(droppedOrder) {
        if (droppedOrder != null) {
            delay(OPTIMISTIC_ORDER_TIMEOUT_MS.milliseconds)
            droppedOrder = null
        }
    }

    DragAutoScrollEffect(
        draggedKey = draggedKey,
        listState = listState,
        pending = pending,
        pendingIds = pendingIds,
        dragOffset = { dragOffset },
        onScrolled = { key, consumed ->
            // Keeps the dragged item under the finger while the list moves beneath it.
            dragOffset += consumed
            dragOffset += moveDraggedItem(key = key, dragOffset = dragOffset, listState = listState, pending = pending)
        },
    )

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = padding1600),
        modifier = modifier.fillMaxSize(),
    ) {
        // Shown on an empty list too, so a locked list can always be unlocked.
        item(key = PENDING_HEADER_KEY) {
            PendingHeader(
                isLocked = state.isLocked,
                onLockToggle = callbacks.onLockToggle,
            )
        }
        if (state.pendingItems.isEmpty() && state.doneItems.isEmpty()) {
            item(key = EMPTY_KEY) {
                FallbackState(
                    title = stringResource(Res.string.shopping_list_empty_title),
                    description = if (state.isLocked) {
                        stringResource(Res.string.shopping_list_empty_locked_description)
                    } else {
                        stringResource(Res.string.shopping_list_empty_description)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (state.canAddItemToHousehold && state.isLocked.not()) {
            item(key = ADD_ITEM_KEY) {
                AddItemRow(
                    householdId = state.currentHouseholdId,
                    onAddItem = callbacks.onAddItem,
                )
            }
        }
        items(items = displayedPending, key = PresentableArticleItem::id) { item ->
            val isDragging = draggedKey == item.id
            Draggable(
                offsetY = if (isDragging) dragOffset else 0f,
                isDragging = isDragging,
                onDragStart = {
                    pending.clear()
                    pending.addAll(state.pendingItems)
                    draggedKey = item.id
                    dragOffset = 0f
                },
                onDrag = { dy ->
                    dragOffset += dy
                    dragOffset += moveDraggedItem(key = item.id, dragOffset = dragOffset, listState = listState, pending = pending)
                },
                onDragEnd = {
                    // Items deleted remotely during the drag are still in the local copy; only the remaining ones count, so
                    // the index matches the shown list and the optimistic order is not discarded (which would snap back).
                    val remaining = pending.filter { it.id in pendingIds }
                    val index = remaining.indexOfFirst { it.id == item.id }
                    droppedOrder = remaining.map { it.id }
                    draggedKey = null
                    dragOffset = 0f
                    if (index != state.pendingItems.indexOfFirst { it.id == item.id }) {
                        callbacks.onItemMoved(item.id, index)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isDragging) Modifier else Modifier.animateItem()),
            ) {
                ArticleRow(item = item, isEditable = state.isLocked.not(), callbacks = callbacks)
            }
        }

        item(key = DONE_HEADER_KEY) {
            AnimatedVisibility(
                visible = state.doneItems.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.padding(top = padding200),
            ) {
                DoneHeader(
                    areCheckedItemsCollapsed = state.areCheckedItemsCollapsed,
                    canClearCheckedItems = state.isLocked.not(),
                    onCheckedItemsCollapse = callbacks.onCheckedItemsCollapse,
                    showClearCheckedItemsConfirmationDialog = callbacks.showClearCheckedItemsConfirmationDialog,
                )
            }
        }

        if (state.areCheckedItemsCollapsed.not()) {
            items(items = state.doneItems, key = { "$DONE_ITEM_TEMPLATE_KEY-${it.id}" }) { item ->
                ArticleRow(
                    item = item,
                    isEditable = state.isLocked.not(),
                    callbacks = callbacks,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

/**
 * Moves the dragged item [key] in [pending] once its center crosses into a neighbouring item.
 * Returns the change of the drag offset that keeps the item under the finger in its new slot.
 */
private fun moveDraggedItem(
    key: String,
    dragOffset: Float,
    listState: LazyListState,
    pending: MutableList<PresentableArticleItem>,
): Float {
    val infos = listState.layoutInfo.visibleItemsInfo
    val current = infos.firstOrNull { it.key == key } ?: return 0f
    val center = current.offset + current.size / 2f + dragOffset
    // Looked up by key, not by LazyColumn index: rows above the pending items (header, add row) come and go, and
    // items deleted remotely during the drag stay in [pending] but are no longer shown.
    val target = infos.firstOrNull { info ->
        info.key != key &&
            pending.any { it.id == info.key } &&
            center >= info.offset &&
            center <= info.offset + info.size
    } ?: return 0f
    val fromIndex = pending.indexOfFirst { it.id == key }
    if (fromIndex == -1) return 0f
    val toIndex = pending.indexOfFirst { it.id == target.key }
    // LazyColumn keeps its scroll position anchored to the key of the first visible item, so moving that item
    // would make the whole list jump with it. Anchoring to the index keeps the list in place.
    listState.requestScrollToItem(
        index = listState.firstVisibleItemIndex,
        scrollOffset = listState.firstVisibleItemScrollOffset,
    )
    pending.add(toIndex, pending.removeAt(fromIndex))
    return (current.offset - target.offset).toFloat()
}

/**
 * Scrolls the list by a fixed step every frame while the dragged item is held near its top or bottom edge.
 * [pendingIds] are the pending items that still exist; [onScrolled] receives the scrolled distance in pixels.
 */
@Composable
private fun DragAutoScrollEffect(
    draggedKey: String?,
    listState: LazyListState,
    pending: List<PresentableArticleItem>,
    pendingIds: Set<String>,
    dragOffset: () -> Float,
    onScrolled: (key: String, consumed: Float) -> Unit,
) {
    val density = LocalDensity.current
    // The effect outlives recompositions during the drag, so it reads the latest ids instead of the ones from its start.
    val currentPendingIds by rememberUpdatedState(pendingIds)
    LaunchedEffect(draggedKey) {
        val key = draggedKey ?: return@LaunchedEffect
        val edgePx = with(density) { AutoScrollEdgeSize.toPx() }
        val stepPx = with(density) { AutoScrollStep.toPx() }
        while (true) {
            withFrameNanos { }
            val layoutInfo = listState.layoutInfo
            val current = layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: continue
            val top = current.offset + dragOffset()
            // Items deleted remotely during the drag are still in the local copy but no longer shown.
            val remaining = pending.filter { it.id in currentPendingIds }
            val pendingIndex = remaining.indexOfFirst { it.id == key }
            // Stops at the first and last pending item, so the dragged item is never scrolled out of the viewport.
            val step = when {
                pendingIndex > 0 && top < layoutInfo.viewportStartOffset + edgePx -> -stepPx
                pendingIndex < remaining.lastIndex && top + current.size > layoutInfo.viewportEndOffset - edgePx -> stepPx
                else -> continue
            }
            onScrolled(key, listState.scrollBy(step))
        }
    }
}

@Composable
private fun ArticleRow(
    item: PresentableArticleItem,
    isEditable: Boolean,
    callbacks: ShoppingListScreenCallbacks,
    modifier: Modifier = Modifier,
) {
    var text by remember(item.id) { mutableStateOf(item.text) }
    var lastSyncedText by remember(item.id) { mutableStateOf(item.text) }

    // Adopt remote edits only when there is no unsent local edit that would be overwritten.
    LaunchedEffect(item.text) {
        if (text == lastSyncedText) text = item.text
        lastSyncedText = item.text
    }
    // Locking the list flushes an edit still waiting for the debounce instead of dropping it.
    LaunchedEffect(text, isEditable) {
        if (text != lastSyncedText) {
            if (text.isNotBlank() && isEditable) delay(TEXT_COMMIT_DEBOUNCE_MS.milliseconds)
            callbacks.onItemTextChange(item.id, text)
        }
    }

    EditableCheckListItem(
        text = text,
        checked = item.isChecked,
        isEditable = isEditable,
        onTextChange = {
            if (it.length <= ARTICLE_TEXT_MAX_LENGTH) {
                text = it
            }
        },
        onCheckedChange = { callbacks.onItemCheckedChange(item.id, it) },
        onDelete = { callbacks.onItemDelete(item.id) },
        singleLine = false,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = modifier,
    )
}

@Composable
private fun AddItemRow(
    householdId: String,
    onAddItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember(householdId) { mutableStateOf("") }
    EditableCheckListItem(
        text = text,
        checked = false,
        onTextChange = {
            if (it.length <= ARTICLE_TEXT_MAX_LENGTH) {
                text = it
            }
        },
        isCheckable = false,
        onCheckedChange = {},
        onDelete = { text = "" },
        singleLine = false,
        placeholder = stringResource(Res.string.shopping_list_add_item_placeholder),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        onKeyboardAction = KeyboardActions(
            onDone = {
                if (text.isNotBlank()) {
                    onAddItem(text)
                } else {
                    this.defaultKeyboardAction(ImeAction.Done)
                }
                text = ""
            },
        ),
        modifier = modifier,
    )
}

@Composable
private fun PendingHeader(
    isLocked: Boolean,
    onLockToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(padding200),
    ) {
        Text(
            text = stringResource(Res.string.shopping_list_pending_section),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f).padding(horizontal = padding400, vertical = padding200),
        )

        IconButton(
            painter = rememberAnimatedLockPainter(isLocked = isLocked),
            onClick = onLockToggle,
            appearance = IconButtonAppearance(
                variant = IconButtonVariant.IconBased,
                size = IconButtonSize.Medium,
                tintColor = MaterialTheme.colorScheme.onBackground,
            ),
        )
    }
}

@Composable
private fun DoneHeader(
    areCheckedItemsCollapsed: Boolean,
    canClearCheckedItems: Boolean,
    onCheckedItemsCollapse: () -> Unit,
    showClearCheckedItemsConfirmationDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCheckedItemsCollapse)
            .padding(padding200),
    ) {
        IconButton(
            painter = rememberAnimatedChevronPainter(isExpanded = areCheckedItemsCollapsed.not()),
            onClick = onCheckedItemsCollapse,
            appearance = IconButtonAppearance(
                variant = IconButtonVariant.IconBased,
                size = IconButtonSize.Medium,
                tintColor = MaterialTheme.colorScheme.onBackground,
            ),
        )

        Text(
            text = stringResource(Res.string.shopping_list_done_section),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f).padding(horizontal = padding400, vertical = padding200),
        )

        if (canClearCheckedItems) {
            IconButton(
                resource = Res.drawable.ic_trash,
                onClick = showClearCheckedItemsConfirmationDialog,
                appearance = IconButtonAppearance(
                    variant = IconButtonVariant.IconBased,
                    size = IconButtonSize.Medium,
                    tintColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        }
    }
}
