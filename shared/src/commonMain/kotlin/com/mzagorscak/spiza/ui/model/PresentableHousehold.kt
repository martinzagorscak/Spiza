package com.mzagorscak.spiza.ui.model

import com.mzagorscak.spiza.domain.model.Household

data class PresentableHousehold(
    val id: String,
    val name: String,
    val description: String,
    val members: List<PresentableHouseholdMember>,
    val ownerId: String,
    val isCurrentUserOwner: Boolean,
)

data class PresentableHouseholdMember(
    val id: String,
    val name: String?,
    val photoUrl: String?,
    val isOwner: Boolean,
    val isCurrentUser: Boolean,
)

fun Household.toPresentable(currentUserId: String?): PresentableHousehold =
    PresentableHousehold(
        id = id,
        name = name,
        description = description,
        members = members.map { user ->
            PresentableHouseholdMember(
                id = user.id,
                name = user.name ?: user.email,
                photoUrl = user.photoUrl,
                isOwner = user.id == ownerId,
                isCurrentUser = user.id == currentUserId,
            )
        },
        ownerId = ownerId,
        isCurrentUserOwner = currentUserId == ownerId,
    )
