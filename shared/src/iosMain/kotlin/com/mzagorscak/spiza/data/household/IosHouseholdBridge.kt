package com.mzagorscak.spiza.data.household

import com.mzagorscak.spiza.data.BridgeError
import com.mzagorscak.spiza.data.household.model.ArticleItemDto
import com.mzagorscak.spiza.data.household.model.HouseholdDto

/** Implemented in Swift (iosApp), which owns FirebaseFirestore. Field names are in [HouseholdFields]. */
interface IosHouseholdBridge {
    fun observeHouseholds(
        userId: String,
        onChange: (List<HouseholdDto>) -> Unit,
        onError: (error: BridgeError) -> Unit,
    ): IosListenerHandle

    fun observeItems(
        householdId: String,
        onChange: (List<ArticleItemDto>) -> Unit,
        onError: (error: BridgeError) -> Unit,
    ): IosListenerHandle

    fun createHouseholdJoinKey(
        userId: String,
        onComplete: (key: String?, error: BridgeError?) -> Unit,
    )

    fun deleteHouseholdJoinKey(
        userId: String,
        onComplete: (error: BridgeError?) -> Unit,
    )

    fun getHouseholdJoinKey(
        userId: String,
        onComplete: (key: String?, error: BridgeError?) -> Unit,
    )

    fun addMemberToHousehold(
        householdId: String,
        userJoinKey: String,
        onComplete: (isUserFound: Boolean, error: BridgeError?) -> Unit,
    )

    fun removeMemberFromHousehold(
        householdId: String,
        memberId: String,
        onComplete: (error: BridgeError?) -> Unit,
    )

    fun deleteHousehold(
        householdId: String,
        onComplete: (error: BridgeError?) -> Unit,
    )

    /** [onComplete] gets the new household id, or an error. */
    fun createHousehold(
        ownerId: String,
        name: String,
        description: String,
        onComplete: (householdId: String?, error: BridgeError?) -> Unit,
    )

    fun updateHousehold(
        householdId: String,
        name: String,
        description: String,
        onComplete: (error: BridgeError?) -> Unit,
    )

    fun addItem(
        householdId: String,
        text: String,
        position: Double,
        onComplete: (error: BridgeError?) -> Unit,
    )

    fun updateItemText(
        householdId: String,
        itemId: String,
        text: String,
        onComplete: (error: BridgeError?) -> Unit,
    )

    fun setItemChecked(
        householdId: String,
        itemId: String,
        isChecked: Boolean,
        onComplete: (error: BridgeError?) -> Unit,
    )

    fun moveItem(
        householdId: String,
        itemId: String,
        position: Double,
        onComplete: (error: BridgeError?) -> Unit,
    )

    fun deleteItem(
        householdId: String,
        itemId: String,
        onComplete: (error: BridgeError?) -> Unit,
    )

    fun clearCheckedItems(
        householdId: String,
        onComplete: (error: BridgeError?) -> Unit,
    )
}

interface IosListenerHandle {
    fun remove()
}
