package com.mzagorscak.spiza.domain.model

data class ShoppingItemSuggestion(
    val id: Long,
    val name: String,
    val normalizedName: String,
    val useCount: Int,
    val lastUsedAt: Long,
)
