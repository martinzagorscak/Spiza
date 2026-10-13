package com.mzagorscak.spiza.ui.screens.shoppingList

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class ShoppingListViewModelImpl : ShoppingListViewModel() {
    private val state = MutableStateFlow(ShoppingListState.Header())

    override fun headerState(): StateFlow<ShoppingListState.Header> = state.asStateFlow()
}
