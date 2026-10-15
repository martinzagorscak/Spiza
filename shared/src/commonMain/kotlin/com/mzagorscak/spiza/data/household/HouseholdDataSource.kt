package com.mzagorscak.spiza.data.household

import com.mzagorscak.spiza.data.household.model.ArticleItemDto
import com.mzagorscak.spiza.data.household.model.HouseholdDto
import kotlinx.coroutines.flow.Flow

/** Streams fail with an exception on listener errors (e.g. permission denied); suspend calls throw on failure. */
interface HouseholdDataSource {
    fun observeHouseholds(userId: String): Flow<List<HouseholdDto>>

    fun observeItems(householdId: String): Flow<List<ArticleItemDto>>

    suspend fun createHousehold(
        ownerId: String,
        name: String,
        description: String,
    ): String

    suspend fun addItem(
        householdId: String,
        text: String,
        position: Double,
    )

    suspend fun updateItemText(
        householdId: String,
        itemId: String,
        text: String,
    )

    suspend fun setItemChecked(
        householdId: String,
        itemId: String,
        isChecked: Boolean,
    )

    suspend fun moveItem(
        householdId: String,
        itemId: String,
        position: Double,
    )

    suspend fun deleteItem(
        householdId: String,
        itemId: String,
    )

    suspend fun clearCheckedItems(householdId: String)
}

object HouseholdFields {
    const val COLLECTION = "households"
    const val ITEMS = "items"
    const val NAME = "name"
    const val DESCRIPTION = "description"
    const val OWNER_ID = "ownerId"
    const val JOINED_MEMBERS = "joinedMembers"
    const val PENDING_MEMBERS = "pendingMembers"
    const val TEXT = "text"
    const val IS_CHECKED = "isChecked"
    const val POSITION = "position"
}
