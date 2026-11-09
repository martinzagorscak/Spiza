package com.mzagorscak.spiza.data.household

import com.mzagorscak.spiza.data.household.model.ArticleItemDto
import com.mzagorscak.spiza.data.household.model.HouseholdDto
import kotlinx.coroutines.flow.Flow

/** Streams fail with an exception on listener errors (e.g. permission denied); suspend calls throw on failure. */
interface HouseholdDataSource {
    suspend fun createHouseholdJoinKey(userId: String): String

    suspend fun deleteHouseholdJoinKey(userId: String)

    fun getHouseholdJoinKey(userId: String): Flow<String?>

    suspend fun getJoinKeyUserId(joinKey: String): String?

    /** Adds [userId] to the household and consumes their [joinKey] in one atomic write. */
    suspend fun addMemberToHousehold(
        householdId: String,
        userId: String,
        joinKey: String,
    )

    suspend fun removeMemberFromHousehold(
        householdId: String,
        memberId: String,
    )

    suspend fun deleteHousehold(householdId: String)

    fun observeHouseholds(userId: String): Flow<List<HouseholdDto>>

    /** One-shot read of the households [userId] has joined, from the server only (never the cache): throws when offline. */
    suspend fun getHouseholds(userId: String): List<HouseholdDto>

    fun observeItems(householdId: String): Flow<List<ArticleItemDto>>

    suspend fun createHousehold(
        ownerId: String,
        name: String,
        description: String,
    ): String

    suspend fun updateHousehold(
        householdId: String,
        name: String,
        description: String,
    )

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
    /** `joinKeys/{joinKey}` -> `{ userId }`: the key is the document id, so it can be fetched by id but never listed. */
    const val COLLECTION_JOIN_KEYS = "joinKeys"
    const val COLLECTION_HOUSEHOLDS = "households"
    const val USER_ID = "userId"
    const val ITEMS = "items"
    const val NAME = "name"
    const val DESCRIPTION = "description"
    const val OWNER_ID = "ownerId"
    const val JOINED_MEMBERS = "joinedMembers"
    const val TEXT = "text"
    const val IS_CHECKED = "isChecked"
    const val POSITION = "position"
}
