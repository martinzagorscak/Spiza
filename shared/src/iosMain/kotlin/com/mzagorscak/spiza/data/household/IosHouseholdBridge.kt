package com.mzagorscak.spiza.data.household

import com.mzagorscak.spiza.data.household.model.ArticleItemDto
import com.mzagorscak.spiza.data.household.model.HouseholdDto

/** Implemented in Swift (iosApp), which owns FirebaseFirestore. Field names are in [HouseholdFields]. */
interface IosHouseholdBridge {
    fun observeHouseholds(
        userId: String,
        onChange: (List<HouseholdDto>) -> Unit,
        onError: (errorMessage: String) -> Unit,
    ): IosListenerHandle

    fun observeItems(
        householdId: String,
        onChange: (List<ArticleItemDto>) -> Unit,
        onError: (errorMessage: String) -> Unit,
    ): IosListenerHandle

    fun createHouseholdJoinKey(
        userId: String,
        onComplete: (key: String?, errorMessage: String?) -> Unit,
    )

    fun deleteHouseholdJoinKey(
        userId: String,
        onComplete: (errorMessage: String?) -> Unit,
    )

    fun getHouseholdJoinKey(
        userId: String,
        onComplete: (key: String?, errorMessage: String?) -> Unit,
    )

    fun addMemberToHousehold(
        householdId: String,
        userJoinKey: String,
        onComplete: (isUserFound: Boolean, errorMessage: String?) -> Unit,
    )

    fun removeMemberFromHousehold(
        householdId: String,
        memberId: String,
        onComplete: (errorMessage: String?) -> Unit,
    )

    fun deleteHousehold(
        householdId: String,
        onComplete: (errorMessage: String?) -> Unit,
    )

    /** [onComplete] gets the new household id, or an error message. */
    fun createHousehold(
        ownerId: String,
        name: String,
        description: String,
        onComplete: (householdId: String?, errorMessage: String?) -> Unit,
    )

    fun updateHousehold(
        householdId: String,
        name: String,
        description: String,
        onComplete: (errorMessage: String?) -> Unit,
    )

    fun addItem(
        householdId: String,
        text: String,
        position: Double,
        onComplete: (errorMessage: String?) -> Unit,
    )

    fun updateItemText(
        householdId: String,
        itemId: String,
        text: String,
        onComplete: (errorMessage: String?) -> Unit,
    )

    fun setItemChecked(
        householdId: String,
        itemId: String,
        isChecked: Boolean,
        onComplete: (errorMessage: String?) -> Unit,
    )

    fun moveItem(
        householdId: String,
        itemId: String,
        position: Double,
        onComplete: (errorMessage: String?) -> Unit,
    )

    fun deleteItem(
        householdId: String,
        itemId: String,
        onComplete: (errorMessage: String?) -> Unit,
    )

    fun clearCheckedItems(
        householdId: String,
        onComplete: (errorMessage: String?) -> Unit,
    )
}

interface IosListenerHandle {
    fun remove()
}
