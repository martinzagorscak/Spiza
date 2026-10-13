package com.mzagorscak.spiza.data.household.model

/** Firestore document `households/{householdId}/items/{id}`. */
data class ArticleItemDto(
    val id: String,
    val text: String,
    val isChecked: Boolean,
    val position: Double,
)
