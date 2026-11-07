package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.domain.model.ArticleItem
import com.mzagorscak.spiza.domain.model.Household
import com.mzagorscak.spiza.domain.repository.HouseholdRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FakeHouseholdRepository : HouseholdRepository {
    val households = MutableStateFlow(listOf(household()))
    val items = MutableStateFlow<List<ArticleItem>>(emptyList())

    /** When set, [observeHouseholds] fails with it (checked each time the stream is collected). */
    var householdsFailure: Throwable? = null

    /** When set, every write throws it. */
    var writeFailure: Throwable? = null

    /** What [addMemberToHousehold] returns; false means no user has that join key. */
    var addMemberResult = true

    data class AddedItem(
        val text: String,
        val position: Double,
        val currentItemsCount: Int,
    )

    val addedItems = mutableListOf<AddedItem>()
    val updatedTexts = mutableListOf<Pair<String, String>>()
    val checkedChanges = mutableListOf<Pair<String, Boolean>>()
    val movedItems = mutableListOf<Pair<String, Double>>()
    val deletedItems = mutableListOf<String>()
    val removedMembers = mutableListOf<String>()
    val deletedHouseholds = mutableListOf<String>()
    val createdHouseholds = mutableListOf<String>()
    val updatedHouseholds = mutableListOf<Pair<String, String>>()

    private fun write() {
        writeFailure?.let { throw it }
    }

    override suspend fun createHouseholdJoinKey(): String? {
        write()
        return "join-key"
    }

    override fun getHouseholdJoinKey(): Flow<String?> = flowOf("join-key")

    override suspend fun refreshHouseholdJoinKey() = write()

    override suspend fun addMemberToHousehold(
        householdId: String,
        userJoinKey: String,
    ): Boolean = addMemberResult

    override suspend fun removeMemberFromHousehold(
        householdId: String,
        memberId: String,
    ) {
        write()
        removedMembers += memberId
    }

    override suspend fun deleteHousehold(householdId: String) {
        write()
        deletedHouseholds += householdId
    }

    override fun observeHouseholds(): Flow<List<Household>> =
        flow {
            householdsFailure?.let { throw it }
            emitAll(households)
        }

    override fun getCachedHouseholdCount(): Flow<Int> = households.map { it.size }

    override fun observeItems(householdId: String): Flow<List<ArticleItem>> = items

    override suspend fun createHousehold(
        name: String,
        description: String,
    ): String? {
        write()
        createdHouseholds += name
        return "household-new"
    }

    override suspend fun updateHousehold(
        householdId: String,
        name: String,
        description: String,
    ) {
        write()
        updatedHouseholds += householdId to name
    }

    override suspend fun addItem(
        householdId: String,
        text: String,
        position: Double,
        currentItemsCount: Int,
    ) {
        write()
        addedItems += AddedItem(text, position, currentItemsCount)
    }

    override suspend fun updateItemText(
        householdId: String,
        itemId: String,
        text: String,
    ) {
        write()
        updatedTexts += itemId to text
    }

    override suspend fun setItemChecked(
        householdId: String,
        itemId: String,
        isChecked: Boolean,
    ) {
        write()
        checkedChanges += itemId to isChecked
    }

    override suspend fun moveItem(
        householdId: String,
        itemId: String,
        position: Double,
    ) {
        write()
        movedItems += itemId to position
    }

    override suspend fun deleteItem(
        householdId: String,
        itemId: String,
    ) {
        write()
        deletedItems += itemId
    }

    override suspend fun clearCheckedItems(householdId: String) = write()
}
