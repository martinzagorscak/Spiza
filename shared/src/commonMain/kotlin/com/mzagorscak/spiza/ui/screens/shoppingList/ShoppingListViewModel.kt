package com.mzagorscak.spiza.ui.screens.shoppingList

import androidx.lifecycle.ViewModel
import com.mzagorscak.spiza.ui.model.PresentableSimpleHousehold
import kotlinx.coroutines.flow.StateFlow

sealed class ShoppingListState {
    data class Header(
        val currentHouseholdName: String? = null,
        val otherHouseholds: List<PresentableSimpleHousehold> = emptyList(),
    ) : ShoppingListState()
}

abstract class ShoppingListViewModel : ViewModel() {
    abstract fun headerState(): StateFlow<ShoppingListState.Header>
}
