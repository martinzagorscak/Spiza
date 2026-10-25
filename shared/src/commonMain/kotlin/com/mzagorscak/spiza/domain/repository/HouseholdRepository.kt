package com.mzagorscak.spiza.domain.repository

import com.mzagorscak.spiza.domain.model.ArticleItem
import com.mzagorscak.spiza.domain.model.Household
import kotlinx.coroutines.flow.Flow

interface HouseholdRepository {
    suspend fun createHouseholdJoinKey(): String?

    fun getHouseholdJoinKey(): Flow<String?>

    suspend fun refreshHouseholdJoinKey()

    suspend fun addMemberToHousehold(
        householdId: String,
        userJoinKey: String,
    ): Boolean

    suspend fun removeMemberFromHousehold(
        householdId: String,
        memberId: String,
    )

    suspend fun deleteHousehold(householdId: String)

    /** Realtime households the signed-in user has joined. Fails the flow when the data source errors. */
    fun observeHouseholds(): Flow<List<Household>>

    /** Returns the cached count of households the signed-in user has joined. Usage:  */
    fun getCachedHouseholdCount(): Flow<Int>

    /** Realtime items of a household, ordered by position. */
    fun observeItems(householdId: String): Flow<List<ArticleItem>>

    /** Returns the id of the created household. */
    suspend fun createHousehold(
        name: String,
        description: String,
    ): String?

    suspend fun updateHousehold(
        householdId: String,
        name: String,
        description: String,
    )

    suspend fun addItem(
        householdId: String,
        text: String,
        position: Double,
        currentItemsCount: Int,
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
