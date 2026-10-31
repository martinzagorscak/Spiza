package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.data.household.HouseholdDataSource
import com.mzagorscak.spiza.data.household.model.ArticleItemDto
import com.mzagorscak.spiza.data.household.model.HouseholdDto
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeHouseholdDataSource : HouseholdDataSource {
    val households = MutableStateFlow<List<HouseholdDto>>(emptyList())
    val items = MutableStateFlow<List<ArticleItemDto>>(emptyList())
    var joinKey: Flow<String?> = flowOf(null)
    var addMemberResult = true

    /** Writes never complete, like Firestore writes while offline. */
    var neverAcknowledgeWrites = false

    val addedMembers = mutableListOf<Pair<String, String>>()
    val addedItems = mutableListOf<String>()
    val createdHouseholds = mutableListOf<String>()
    val updatedHouseholds = mutableListOf<String>()
    val deletedJoinKeysOfUsers = mutableListOf<String>()

    private suspend fun write() {
        if (neverAcknowledgeWrites) awaitCancellation()
    }

    override suspend fun createHouseholdJoinKey(userId: String): String {
        write()
        return "join-key"
    }

    override suspend fun deleteHouseholdJoinKey(userId: String) {
        deletedJoinKeysOfUsers += userId
    }

    override fun getHouseholdJoinKey(userId: String): Flow<String?> = joinKey

    override suspend fun addMemberToHousehold(
        householdId: String,
        userJoinKey: String,
    ): Boolean {
        write()
        addedMembers += householdId to userJoinKey
        return addMemberResult
    }

    override suspend fun removeMemberFromHousehold(
        householdId: String,
        memberId: String,
    ) = write()

    override suspend fun deleteHousehold(householdId: String) = write()

    override fun observeHouseholds(userId: String): Flow<List<HouseholdDto>> = households

    override fun observeItems(householdId: String): Flow<List<ArticleItemDto>> = items

    override suspend fun createHousehold(
        ownerId: String,
        name: String,
        description: String,
    ): String {
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
        updatedHouseholds += householdId
    }

    override suspend fun addItem(
        householdId: String,
        text: String,
        position: Double,
    ) {
        write()
        addedItems += text
    }

    override suspend fun updateItemText(
        householdId: String,
        itemId: String,
        text: String,
    ) = write()

    override suspend fun setItemChecked(
        householdId: String,
        itemId: String,
        isChecked: Boolean,
    ) = write()

    override suspend fun moveItem(
        householdId: String,
        itemId: String,
        position: Double,
    ) = write()

    override suspend fun deleteItem(
        householdId: String,
        itemId: String,
    ) = write()

    override suspend fun clearCheckedItems(householdId: String) = write()
}
