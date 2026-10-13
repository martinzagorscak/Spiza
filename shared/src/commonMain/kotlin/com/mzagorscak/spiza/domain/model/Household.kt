package com.mzagorscak.spiza.domain.model

data class Household(
    val id: String,
    val name: String,
    val description: String,
    val ownerId: String,
    val joinedMembers: List<String>,
    val pendingMembers: List<String>,
)

/** Items of one household live in a single list; [isChecked] decides whether it is pending or done. */
data class ArticleItem(
    val id: String,
    val text: String,
    val isChecked: Boolean,
    val position: Double,
)
