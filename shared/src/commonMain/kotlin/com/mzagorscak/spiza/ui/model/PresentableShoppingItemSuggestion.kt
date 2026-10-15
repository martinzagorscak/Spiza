package com.mzagorscak.spiza.ui.model

import com.mzagorscak.spiza.domain.model.ShoppingItemSuggestion

data class PresentableShoppingItemSuggestion(
    val id: Long,
    val name: String,
    val isEnabled: Boolean = true,
)

fun List<ShoppingItemSuggestion>.toPresentable(): List<PresentableShoppingItemSuggestion> = map { it.toPresentable() }

fun ShoppingItemSuggestion.toPresentable(isEnabled: Boolean = true): PresentableShoppingItemSuggestion =
    PresentableShoppingItemSuggestion(
        id = id,
        name = name,
        isEnabled = isEnabled,
    )
