package com.mzagorscak.spiza.ui.screens.shoppingList

import androidx.lifecycle.ViewModel
import com.mzagorscak.spiza.ui.model.PresentableArticleItem
import com.mzagorscak.spiza.ui.model.PresentableShoppingItemSuggestion
import com.mzagorscak.spiza.ui.model.PresentableSimpleHousehold
import kotlinx.coroutines.flow.StateFlow

sealed class ShoppingListState {
    enum class DialogState {
        Hidden,
        ClearCheckedItemsConfirmation,
    }

    data class Header(
        val currentHouseholdName: String? = null,
        val otherHouseholds: List<PresentableSimpleHousehold> = emptyList(),
    ) : ShoppingListState()

    sealed class Body : ShoppingListState() {
        data object Loading : Body()

        data object Error : Body()

        data object NoHousehold : Body()

        data class Content(
            val pendingItems: List<PresentableArticleItem> = emptyList(),
            val doneItems: List<PresentableArticleItem> = emptyList(),
            val suggestions: List<PresentableShoppingItemSuggestion> = emptyList(),
            val areCheckedItemsCollapsed: Boolean = false,
            val dialogState: DialogState = DialogState.Hidden,
            val hasWriteError: Boolean = false,
        ) : Body()
    }
}

abstract class ShoppingListViewModel : ViewModel() {
    abstract fun headerState(): StateFlow<ShoppingListState.Header>

    abstract fun bodyState(): StateFlow<ShoppingListState.Body>

    abstract fun onRetryClicked()

    abstract fun onHouseholdSelected(householdId: String)

    abstract fun onAddItem(text: String)

    abstract fun onItemTextChange(
        itemId: String,
        text: String,
    )

    abstract fun onItemCheckedChange(
        itemId: String,
        isChecked: Boolean,
    )

    /** [toIndex] is the index among the pending items after the move. */
    abstract fun onItemMoved(
        itemId: String,
        toIndex: Int,
    )

    abstract fun onItemDelete(itemId: String)

    abstract fun onCheckedItemsCollapse()

    abstract fun showClearCheckedItemsConfirmationDialog()

    abstract fun dismissDialog()

    abstract fun clearCheckedItems()

    abstract fun onWriteErrorShown()
}
