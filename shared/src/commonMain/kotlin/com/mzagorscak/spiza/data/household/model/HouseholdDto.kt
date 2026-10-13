package com.mzagorscak.spiza.data.household.model

/** Firestore document `households/{id}`. */
data class HouseholdDto(
    val id: String,
    val name: String,
    val description: String,
    val ownerId: String,
    val joinedMembers: List<String>,
    val pendingMembers: List<String>,
)
